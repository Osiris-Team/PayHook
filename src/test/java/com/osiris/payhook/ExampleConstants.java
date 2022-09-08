package com.osiris.payhook;

public class ExampleConstants {
    public static Product pCoolCookie;
    public static Product pCoolSubscription;

    // Insert the below somewhere where it gets ran once.
    // For example in the static constructor of a Constants class of yours, like shown here, or in your main method.
    static {
        try {
            PayHook.init(
                    "Brand-Name",
                    "db_url",
                    "db_username",
                    "db_password",
                    true,
                    "https://my-shop.com/payment/success",
                    "https://my-shop.com/payment/cancel");

            PayHook.initBraintree("merchant_id", "public_key", "private_key", "https://my-shop.com/braintree-hook");
            PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

            pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Interval.NONE);
            pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Interval.MONTHLY);

            PayHook.onPaymentAuthorized.addAction(event -> {
                // Additional backend business logic for all payments in here.
                // Gets executed every time a payment is authorized/completed.
                // If something goes wrong in here a RuntimeException is thrown.
                Product product = event.product;
                Payment payment = event.payment;
            });

            PayHook.onPaymentCancelled.addAction(event -> {
                // Additional backend business logic for all payments in here.
                // Gets executed every time a payment was cancelled.
                // If something goes wrong in here a RuntimeException is thrown.
                Product product = event.product;
                Payment payment = event.payment;
            });

            // The cleaner thread is only needed
            // to remove the added actions from further below, since those may not get executed once.
            // Remove them after 6-7 hours.
            PayHook.onPaymentAuthorized.initCleaner(3600000, obj -> { // Check every hour
                return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
            }, Exception::printStackTrace);
            PayHook.onPaymentCancelled.initCleaner(3600000, obj -> { // Check every hour
                return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
            }, Exception::printStackTrace);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This can be anywhere in your application.
     */
    void onBuyBtnClick() throws Exception {
        Payment payment = PayHook.expectPayment("USER_ID", pCoolCookie, PaymentProcessor.PAYPAL);
        // Forward your user to payment.url to complete/authorize the payment here...

        PayHook.onPaymentAuthorized.addAction((action, event) -> {
            if (event.payment.id == payment.id) {
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment authorizedPayment = event.payment;
                // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                // Code that does backend, aka important stuff does not belong here!
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();

        PayHook.onPaymentCancelled.addAction((action, event) -> {
            if (event.payment.id == payment.id) {
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment cancelledPayment = event.payment;
                // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                // Code that does backend, aka important stuff does not belong here!
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();
    }

    /**
     * This can be anywhere in your application.
     */
    void onAnotherBuyBtnClick() throws Exception {
        Payment payment = PayHook.expectPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE);
        // Forward your user to payment.url to complete/authorize the payment here...

        PayHook.onPaymentAuthorized.addAction((action, event) -> {
            if (event.payment.id == payment.id) {
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment authorizedPayment = event.payment;
                // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                // Code that does backend, aka important stuff does not belong here!
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();

        PayHook.onPaymentCancelled.addAction((action, event) -> {
            if (event.payment.id == payment.id) {
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment cancelledPayment = event.payment;
                // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                // Code that does backend, aka important stuff does not belong here!
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();
    }
}
