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
            P = new PayHookV3("db_url",
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
    }

    void onBuyBtnClick(){
        // The code below should be run when the user clicks on a buy button.
        Order order1 = P.createStripeOrder(product);
        Order order2 = P.createStripeOrder(productRecurring);

        order1.onPaymentReceived(event -> { // Note that this only gets ran once

        });

        order2.onPaymentReceived(event -> {

        });
    }
}
