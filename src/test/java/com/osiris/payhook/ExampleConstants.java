package com.osiris.payhook;

import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.jsqlgen.payhook.Product;

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
                    "db_name",
                    "db_username",
                    "db_password",
                    true,
                    "https://my-shop.com/payment/success",
                    "https://my-shop.com/payment/cancel");

            PayHook.initBraintree("merchant_id", "public_key", "private_key", "https://my-shop.com/braintree-hook");
            PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

            pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Interval.NONE);
            pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Interval.MONTHLY);

            PayHook.onPaymentAuthorized.addAction(payment -> {
                // Additional backend business logic for all payments in here.
                // Gets executed every time a payment is authorized/completed.
                // If something goes wrong in here a RuntimeException is thrown.
            });

            PayHook.onPaymentCancelled.addAction(payment -> {
                // Additional backend business logic for all payments in here.
                // Gets executed every time a payment was cancelled.
                // If something goes wrong in here a RuntimeException is thrown.
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This can be anywhere in your application.
     */
    void onAnotherBuyBtnClick() throws Exception {
        Payment payment = PayHook.expectPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE,
                authorizedPayment -> {
                    // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                    // Code that does backend, aka important stuff does not belong here!
                }, cancelledPayment -> {
                    // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                    // Code that does backend, aka important stuff does not belong here!
                });
        // Forward your user to payment.url to complete/authorize the payment here...
    }
}
