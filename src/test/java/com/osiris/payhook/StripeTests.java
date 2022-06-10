package com.osiris.payhook;

import org.junit.jupiter.api.Test;

public class StripeTests {
    public static Product pCoolCookie;
    public static Product pCoolSubscription;
    @Test
    void test() throws Exception {
        TestUtils.fetchCrendentials();
        PayHook.init(
                "Brand-Name",
                TestUtils.dbUrl,
                TestUtils.dbUsername,
                TestUtils.dbPassword,
                true);
        String webhookUrl = "https://webhook.site/#!/b59f24a1-36c1-4af3-871d-d411586b82ff"; // TODO change this
        PayHook.initStripe(TestUtils.stripeSecretKey, webhookUrl);

        pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Type.ONE_TIME, 0);
        pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Type.RECURRING, 0);

        PayHook.onExpiredPayment(event -> {
            // Executed when the user misses the payment for a subscription (recurring).
            try{
                Product product = event.product;
                Payment payment = event.payment;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
