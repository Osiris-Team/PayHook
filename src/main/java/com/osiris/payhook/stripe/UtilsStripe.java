package com.osiris.payhook.stripe;

import com.google.gson.JsonSyntaxException;
import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.jsqlgen.payhook.Product;
import com.osiris.payhook.PayHook;
import com.osiris.payhook.PaymentProcessor;
import com.osiris.payhook.Subscription;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import java.util.List;
import java.util.Map;

public class UtilsStripe {

    /**
     * Checks if the provided webhook event is actually a real/valid one.
     *
     * @return null, if not valid.
     */
    public Event checkWebhookEvent(String body, Map<String, String> headers, String endpointSecret)
            throws JsonSyntaxException, SignatureVerificationException { // Invalid payload // Invalid signature
        String sigHeader = headers.get("Stripe-Signature");
        if (sigHeader == null) {
            sigHeader = headers.get("stripe-signature"); // try lowercase
            if (sigHeader == null)
                throw new SignatureVerificationException("No Stripe-Signature/stripe-signature header present!", "---");
        }
        return Webhook.constructEvent(body, sigHeader, endpointSecret);
    }

    public void handleEvent(Map<String, String> header, String body, String stripeWebhookSecret) throws Exception {
        long now = System.currentTimeMillis();
        Event event = checkWebhookEvent(body, header, stripeWebhookSecret);
        if (event == null)
            throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", validation failed).");
        // Deserialize the nested object inside the event
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
            // instructions on how to handle this case, or return an error here.
            throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", deserialization failed).");
        }

        // Handle the event
        String type = event.getType();
        if ("checkout.session.completed".equals(type)) {// Checkout payment was authorized/completed
            Session session = (Session) stripeObject;

            List<Payment> paymentsWithSessionId = Payment.whereStripeSessionId().is(session.getId()).get();
            if (paymentsWithSessionId.isEmpty())
                throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find session id '" + session.getId() + "' in local database).");
            long totalCharge = 0;
            for (Payment p : paymentsWithSessionId) {
                totalCharge += p.charge;
            }
            if (totalCharge != session.getAmountTotal())
                throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", expected paid amount of '" + totalCharge + "' but got '" + session.getAmountTotal() + "').");

            if (session.getSubscription() != null) { // Subscription was just bought
                if (paymentsWithSessionId.size() != 1)
                    throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", wrong amount of payments (" + paymentsWithSessionId.size() + ") for subscription that was just created," +
                            " expected 1 with stripe session id " + session.getId() + ").");

                Payment firstPayment = paymentsWithSessionId.get(0);
                firstPayment.stripeSubscriptionId = session.getSubscription();
                firstPayment.stripePaymentIntentId = Invoice.retrieve(com.stripe.model.Subscription.retrieve(session.getSubscription()).getLatestInvoice()).getPaymentIntent();
                firstPayment.timestampAuthorized = now;

                Payment.update(firstPayment);
                PayHook.onPaymentAuthorized.execute(firstPayment);
            } else { // Authorized/Completed one-time payment(s)
                for (Payment payment : paymentsWithSessionId) {
                    if (payment.timestampAuthorized == 0) {
                        payment.timestampAuthorized = now;
                        payment.stripePaymentIntentId = session.getPaymentIntent();
                        Payment.update(payment);
                        PayHook.onPaymentAuthorized.execute(payment);
                    }
                }
            }
        } else if ("invoice.created".equals(type)) {// Recurring payments
            // Return 2xx status code to auto-finalize the invoice and receive an invoice.paid event next
        } else if ("invoice.paid".equals(type)) {// Recurring payments
            Invoice invoice = (Invoice) stripeObject;
            String subscriptionId = invoice.getSubscription();
            if (subscriptionId == null)
                return; // Make sure NOT recurring payments are ignored (handled by checkout.session.completed)
            if (invoice.getBillingReason().equals("subscription_create")) return; // Also ignore
            // the first payment/invoice for a subscription, because that MUST be handled by checkout.session.completed,
            // because this event has no information about the checkout session id.

            List<Payment> authorizedPayments = Payment.getAuthorizedPayments("stripeSubscriptionId = ?", subscriptionId);
            if (authorizedPayments.isEmpty()) throw new WebHookValidationException(
                    "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find authorized payments with stripeSubscriptionId '" + subscriptionId + "' in local database).");
            Payment lastPayment = authorizedPayments.get(authorizedPayments.size() - 1);
            Product product = Product.get(lastPayment.productId);
            if (product.charge != invoice.getAmountPaid())
                throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", expected paid amount of '" + product.charge + "' but got '" + invoice.getAmountPaid() + "').");
            Payment newPayment = lastPayment.clone();
            newPayment.id = Payment.create(lastPayment.userId, invoice.getAmountPaid(), product.currency, product.paymentInterval)
                    .id;
            newPayment.url = null;
            newPayment.charge = invoice.getAmountPaid();
            newPayment.timestampCreated = now;
            newPayment.timestampAuthorized = now;
            newPayment.timestampRefunded = 0;
            newPayment.timestampExpires = now + 100000;
            newPayment.timestampCancelled = 0;
            Payment.add(newPayment);
            PayHook.onPaymentAuthorized.execute(newPayment);
        } else if ("customer.subscription.deleted".equals(type)) {// Recurring payments
            com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject; // TODO check if this actually works
            List<Payment> payments = Payment.whereStripeSubscriptionId().is(subscription.getId()).get();
            if (payments.isEmpty()) throw new WebHookValidationException(
                    "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find payments with stripe_subscription_id '" + subscription.getId() + "' in local database).");
            new Subscription(payments).cancel();
        } else if ("charge.refunded".equals(type)) {// Occurs whenever a charge is refunded, including partial refunds.
            Charge charge = (Charge) stripeObject;
            List<Payment> payments = Payment.whereStripePaymentIntentId().is(charge.getPaymentIntent()).get();
            if (payments.isEmpty()) throw new WebHookValidationException(
                    "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find payments with stripe_payment_intent_id '" + charge.getPaymentIntent() + "' in local database).");
            PayHook.receiveRefund(charge.getAmount(), payments);
        } else {
            throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", invalid event-type: " + event.getType() + ").");
        }
    }
}
