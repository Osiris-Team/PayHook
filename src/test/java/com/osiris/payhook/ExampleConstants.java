package com.osiris.payhook;

import java.sql.SQLException;

public class ExampleConstants {
    public static final PayHook P;
    public static final Product product;
    public static final Product productRecurring;

    static {
        // Insert the below somewhere where it gets ran once.
        // For example in a Constants class of yours.
        try {
            P = new PayHook(
                    "Brand-Name",
                    "db_url",
                    "db_username",
                    "db_password",
                    true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        P.initPayPal(true, "client_id", "client_secret");
        P.initStripe(true, "secret_key");

        product = P.putProduct();
        productRecurring = P.putProduct();

        P.onMissedPayment(event -> {
            // Executed when the user misses the payment for a subscription (recurring).
            try{
                Product product = event.product;
                Payment payment = event.payment;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    void onBuyBtnClick() throws Exception {
        // The code below should be run when the user clicks on a buy button.
        Payment payment = P.createPayment("USER_ID", product, PaymentProcessor.PAYPAL, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        P.onPayment(payment.paymentId, event -> {
            // Executed when the payment was received.
        });
    }

    void onAnotherBuyBtnClick() throws Exception {
        Payment payment = P.createPayment("USER_ID", productRecurring, PaymentProcessor.STRIPE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        P.onPayment(payment.paymentId, event -> {
            // Executed when the payment was received.
        });
    }
}
