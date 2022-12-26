package com.osiris.payhook;

import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.payhook.utils.Converter;
import com.osiris.payhook.utils.UtilsTime;

import java.util.*;

public class Subscription {
    public List<Payment> payments;

    /**
     * @param payments must all have the same subscription id.
     */
    public Subscription(Payment... payments) {
        this.payments = new ArrayList<>(Arrays.asList(payments)); // Copy since asList is unmodifiable
    }

    /**
     * @param payments must all have the same subscription id.
     */
    public Subscription(List<Payment> payments) {
        this.payments = payments;
    }

    public static List<Subscription> getForUser(String userId) {
        //TODO ADD PAYMENT PROCESSOR
        List<Payment> payments = Payment.get("WHERE userId=? AND (paypalSubscriptionId IS NOT NULL OR stripeSubscriptionId IS NOT NULL)"
                , userId);
        return paymentsToSubscriptions(payments);
    }

    public static List<Subscription> get() {
        //TODO ADD PAYMENT PROCESSOR
        List<Payment> payments = Payment.get("WHERE paypalSubscriptionId IS NOT NULL OR stripeSubscriptionId IS NOT NULL");
        return paymentsToSubscriptions(payments);
    }

    public static List<Subscription> getNotCancelled() {
        //TODO ADD PAYMENT PROCESSOR
        List<Payment> payments = Payment.get("WHERE paypalSubscriptionId IS NOT NULL OR stripeSubscriptionId IS NOT NULL");
        List<Subscription> subs = paymentsToSubscriptions(payments);
        List<Subscription> subsToRemove = new ArrayList<>();
        for (Subscription sub : subs) {
            if (sub.isCancelled())
                subsToRemove.add(sub);
            else if (sub.getMillisLeft() < 0)
                subsToRemove.add(sub);
        }
        for (Subscription sub : subsToRemove) {
            subs.remove(sub);
        }
        return subs;
    }

    public static List<Subscription> paymentsToSubscriptions(List<Payment> payments) {
        //TODO ADD PAYMENT PROCESSOR
        Map<String, List<Payment>> paypalSubs = new HashMap<>();
        Map<String, List<Payment>> stripeSubs = new HashMap<>();
        for (Payment payment : payments) {
            if (payment.paypalSubscriptionId != null) {
                List<Payment> list = paypalSubs.computeIfAbsent(payment.paypalSubscriptionId, k -> new ArrayList<>());
                list.add(payment);
            }
        }
        for (Payment payment : payments) {
            if (payment.stripeSubscriptionId != null) {
                List<Payment> list = stripeSubs.computeIfAbsent(payment.stripeSubscriptionId, k -> new ArrayList<>());
                list.add(payment);
            }
        }

        List<Subscription> subs = new ArrayList<>();
        paypalSubs.forEach((id, listPayments) -> {
            subs.add(new Subscription(listPayments));
        });
        stripeSubs.forEach((id, listPayments) -> {
            subs.add(new Subscription(listPayments));
        });
        return subs;
    }

    public Subscription refund() {

        return this;
    }

    public Subscription cancel() throws Exception {
        Payment lastPayment = getLastPayment();
        PayHook.cancelPayment(lastPayment);
        return this;
    }

    public Payment getLastPayment() {
        return payments.get(payments.size() - 1);
    }

    /**
     * Calculates the amount of milliseconds this subscription is still valid for. <br>
     * If the customer doesn't pay in time, this method returns a negative value. <br>
     * Ignores {@link Payment#timestampCancelled} and relies on {@link Payment#timestampAuthorized}
     * of the last {@link Payment}.
     */
    public long getMillisLeft() {
        Payment lastAuthorizedPayment = getLastAuthorizedPayment();
        if (lastAuthorizedPayment == null) return 0;
        long totalMillisValid = Payment.Interval.toMilliseconds(lastAuthorizedPayment.interval);
        return totalMillisValid - (System.currentTimeMillis() - lastAuthorizedPayment.timestampAuthorized);
    }

    /**
     * Same as {@link #getMillisLeft()} but adds the payment processor
     * specific puffer: {@link PayHook#paypalUrlTimeoutMs} or
     * {@link PayHook#stripeUrlTimeoutMs}.
     */
    public long getMillisLeftWithPuffer() {
        Payment lastAuthorizedPayment = getLastAuthorizedPayment();
        if (lastAuthorizedPayment == null) return 0;
        if (lastAuthorizedPayment.stripeSubscriptionId != null)
            return getMillisLeft() + PayHook.stripeUrlTimeoutMs;
        else if (lastAuthorizedPayment.paypalSubscriptionId != null)
            return getMillisLeft() + PayHook.paypalUrlTimeoutMs;
        else
            throw new IllegalStateException("Last payment of subscription must have a payment-processor specific subscription id!");
        //TODO ADD PAYMENT PROCESSOR
    }

    public boolean isCancelled() {
        return getLastPayment().timestampCancelled != 0;
    }

    public boolean isPaid() {
        return getMillisLeft() > 0;
    }

    public Payment getLastAuthorizedPayment() {
        for (int i = payments.size() - 1; i > -1; i--) {
            Payment payment = payments.get(i);
            if (payment.isAuthorized()) {
                return payment;
            }
        }
        return null;
    }

    public long getTotalPaid() {
        long cents = 0;
        for (Payment payment : payments) {
            cents += payment.charge;
        }
        return cents;
    }

    public String toPrintString() {
        Payment lastPayment = getLastPayment();
        long millisLeft = getMillisLeft();
        long millisLeftWithPuffer = getMillisLeftWithPuffer();
        return "userid=" + lastPayment.userId + " payments=" + payments.size() + "" +
                " time-left=" + (millisLeft > 0 ? new UtilsTime().getFormattedString(millisLeft) : "")
                + " time-left-with-puffer=" + (millisLeftWithPuffer > 0 ? new UtilsTime().getFormattedString(millisLeftWithPuffer) : "")
                + " last-authorized-payment=" + (lastPayment.timestampAuthorized > 0 ? new Date(lastPayment.timestampAuthorized) : "")
                + " total-paid=" + new Converter().toMoneyString(lastPayment.currency, getTotalPaid());
    }
}
