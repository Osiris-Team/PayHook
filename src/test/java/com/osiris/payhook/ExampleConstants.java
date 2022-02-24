package com.osiris.payhook;

import java.sql.SQLException;

public class ExampleConstants {
    public static final PayHookV3 P;
    public static final Product product;
    public static final Product productRecurring;

    static {
        // Insert the below somewhere where it gets ran once.
        // For example in a Constants class of yours.
        try {
            P = new PayHookV3(
                    "payhook",
                    "db_url",
                    "db_name",
                    "db_password");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        P.initPayPal(true, "client_id", "client_secret");
        P.initStripe(true, "secret_key");

        // These should be made public static fields in your Constants class
        product = P.createProduct();
        productRecurring = P.createProduct();

        P.onMissedPayment(event -> { // Relevant if you have products with recurring payments (like subscriptions)
            try{
                Order o = event.getOrder();
                // TODO what should happen when the user misses a payment?
                // TODO implement logic.
            } catch (Exception e) {
                // TODO handle exception
                e.printStackTrace();
            }
        });

        P.onException(e -> {
           // TODO handle exception
        });

        P.runPayPalPaymentReceived();
        P.runStripePaymentReceived();
    }

    void onBuyBtnClick(){
        // The code below should be run when the user clicks on a buy button.
        Order order = P.createOrder("https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel", product);
        order.onPaymentReceived(event -> { // Note that this only gets ran once

        });
    }

    void onAnotherBuyBtnClick(){
        Order order = P.createStripeOrder(productRecurring);
        order.onPaymentReceived(event -> {

        });
    }
}
