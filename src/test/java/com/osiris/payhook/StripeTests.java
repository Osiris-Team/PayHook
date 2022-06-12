package com.osiris.payhook;

public class StripeTests {
    public static Product pCoolCookie;
    public static Product pCoolSubscription;
    @org.junit.jupiter.api.Test
    void test() throws Exception {
        TestUtils.init();
        PayHook.init(
                "Brand-Name",
                TestUtils.dbUrl,
                TestUtils.dbUsername,
                TestUtils.dbPassword,
                true);
        String webhookUrl = "https://webhook.site/#!/b59f24a1-36c1-4af3-871d-d411586b82ff"; // TODO change this
        PayHook.initStripe(TestUtils.stripeSecretKey, webhookUrl);

        pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE, 0);
        pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.DAYS_30, 0);

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
