package com.osiris.payhook;

public class ExampleConstants {
    public static Product pCoolCookie;
    public static Product pCoolSubscription;

    // Insert the below somewhere where it gets ran once.
    // For example in the static constructor of a Constants class of yours, like shown here.
    static {
        try {
            PayHook.init(
                    "Brand-Name",
                    "db_url",
                    "db_username",
                    "db_password",
                    true);

            PayHook.initBraintree("merchant_id","public_key", "private_key", "https://my-shop.com/braintree-hook");
            PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

            pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE);
            pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.MONTHLY);

            PayHook.paymentAuthorizedEvent.addAction((action, event) -> {
                // Backend business logic in here. Gets executed every time.
                Product product = event.product;
                Payment payment = event.payment;
                }, e -> {
                e.printStackTrace();
            });

            PayHook.paymentCancelledEvent.addAction((action, event) -> {
                // Backend business logic in here. Gets executed every time.
                Product product = event.product;
                Payment payment = event.payment;
                }, e -> {
                e.printStackTrace();
            });

            // The cleaner thread is only needed
            // to remove the added actions from further below,
            // since those may not get executed once.
            // Remove them after 6 hours
            PayHook.paymentAuthorizedEvent.initCleaner(3600000, obj -> { // Check every hour
                return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
            }, Exception::printStackTrace);
            PayHook.paymentCancelledEvent.initCleaner(3600000, obj -> { // Check every hour
                return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
            }, Exception::printStackTrace);

        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This can be anywhere in your application.
     */
    void onBuyBtnClick() throws Exception {
        Payment payment = PayHook.createPayment("USER_ID", pCoolCookie, PaymentProcessor.BRAINTREE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        // Forward your user to payment.url
        PayHook.paymentAuthorizedEvent.addAction((action, event) -> {
            if(event.payment.id == payment.id){
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment authorizedPayment = event.payment;
                // Additional UI code here (make sure to have access to the UI thread).
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();

        PayHook.paymentCancelledEvent.addAction((action, event) -> {
            if(event.payment.id == payment.id){
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment cancelledPayment = event.payment;
                // Additional UI code here (make sure to have access to the UI thread).
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();
    }

    /**
     * This can be anywhere in your application.
     */
    void onAnotherBuyBtnClick() throws Exception {
        Payment payment = PayHook.createPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        // Forward your user to payment.url
        PayHook.paymentAuthorizedEvent.addAction((action, event) -> {
            if(event.payment.id == payment.id){
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment authorizedPayment = event.payment;
                // Additional UI code here (make sure to have access to the UI thread).
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();

        PayHook.paymentCancelledEvent.addAction((action, event) -> {
            if(event.payment.id == payment.id){
                action.remove(); // To make sure it only gets executed once, for this payment.
                Product product = event.product;
                Payment cancelledPayment = event.payment;
                // Additional UI code here (make sure to have access to the UI thread).
            }
        }, e -> {
            e.printStackTrace();
        }).object = System.currentTimeMillis();
    }
}
