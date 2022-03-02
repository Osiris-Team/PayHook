package com.osiris.payhook;

import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.stripe.exception.StripeException;

import java.io.IOException;
import java.sql.SQLException;

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

            pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", PaymentType.ONE_TIME, 0);
            pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", PaymentType.RECURRING, 0);

            PayHook.onMissedPayment(event -> {
                // Executed when the user misses the payment for a subscription (recurring).
                try{
                    Product product = event.product;
                    Payment payment = event.payment;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (StripeException | IOException | HttpErrorException e) {
            e.printStackTrace();
        }
    }

    void onBuyBtnClick() throws Exception {
        // The code below should be run when the user clicks on a buy button.
        Payment payment = PayHook.createPayment("USER_ID", pCoolCookie, PaymentProcessor.BRAINTREE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        PayHook.onReceivedPayment(payment.paymentId, event -> {
            // Executed when the payment was received.
        });
    }

    void onAnotherBuyBtnClick() throws Exception {
        Payment payment = PayHook.createPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        PayHook.onReceivedPayment(payment.paymentId, event -> {
            // Executed when the payment was received.
        });
    }
}
