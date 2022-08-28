package com.osiris.payhook;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.dyml.Yaml;
import com.osiris.payhook.utils.Converter;
import com.stripe.model.WebhookEndpoint;
import io.muserver.*;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class GeneralTest {
    public static SQLTestServer dbServer;
    public static String dbUrl;
    public static String dbUsername = "root";
    public static String dbPassword = "";
    public static String stripeSecretKey = null;
    private static Product pCoolCookie;
    private static Product pCoolSubscription;

    /**
     * A general test that tests product creation/updating
     * for all currently supported payment processors
     * and payment creation/cancellation. (sandbox mode strictly) <br>
     * Note that this will delete old
     * webhooks that contain ngrok.io in their url. <br>
     */
    @Test
    public void run() throws Exception {

        // Fetch test credentials
        Yaml yaml = new Yaml(System.getProperty("user.dir") + "/test-credentials.yml");
        System.out.println("Fetching credentials...");
        System.out.println("File: " + yaml.file);
        yaml.load();
        String ngrokAuthToken = yaml.put("ngrok auth token").asString();
        String stripeSecretKey = yaml.put("stripe secret key").asString();
        String paypalClientId = yaml.put("paypal client id").asString();
        String paypalClientSecret = yaml.put("paypal client secret").asString();
        yaml.save();
        System.out.println("OK!");

        // Test credentials check
        System.out.println("Checking config values (credentials cannot be null)... ");
        Objects.requireNonNull(ngrokAuthToken, "ngrokAuthToken cannot be null!");
        Objects.requireNonNull(stripeSecretKey, "stripeSecretKey cannot be null!");
        Objects.requireNonNull(paypalClientId, "paypalClientId cannot be null!");
        Objects.requireNonNull(paypalClientSecret, "paypalClientSecret cannot be null!");
        System.out.println("OK!");

        // Init web-server to listen for webhook events
        System.out.println("Starting web-server...");
        MuServer server = MuServerBuilder.httpServer()
                .withHttpPort(80)
                .addHandler(Method.GET, "/", (request, response, pathParams) -> {
                    response.write("Currently running from " + this);
                })
                .addHandler(Method.POST, "/paypal-hook", this::doPayPalWebhookEvent)
                .addHandler(Method.POST, "/stripe-hook", this::doStripeWebhookEvent)
                .start();
        System.out.println("Started web-server at " + server.uri());
        System.out.println("OK!");

        // Setup ngrok to tunnel traffic from public ip the current locally running service/app
        // Open a HTTP tunnel on the default port 80
        // <Tunnel: "http://<public_sub>.ngrok.io" -> "http://localhost:80">
        System.out.println("Starting Ngrok-Client...");
        final NgrokClient ngrokClient = new NgrokClient.Builder()
                .withJavaNgrokConfig(new JavaNgrokConfig.Builder().withAuthToken(ngrokAuthToken).build())
                .build();
        final Tunnel httpTunnel = ngrokClient.connect(new CreateTunnel.Builder().withBindTls(true).build());
        String baseUrl = httpTunnel.getPublicUrl();
        String stripeWebhookUrl = baseUrl + "/stripe-hook";
        String paypalWebhookUrl = baseUrl + "/paypal-hook";
        System.out.println("Public baseUrl: " + baseUrl);
        System.out.println("Public stripeWebhookUrl: " + stripeWebhookUrl);
        System.out.println("Public paypalWebhookUrl: " + paypalWebhookUrl);
        System.out.println("Now forwarding traffic from " + baseUrl + " to " + server.uri());
        System.out.println("OK!");

        // Init test database without password
        System.out.println("Starting database...");
        dbServer = SQLTestServer.buildAndRun();
        dbUrl = dbServer.getUrl();
        Database.create();
        System.out.println("Url: " + dbUrl);
        System.out.println("OK!");

        // Initialise payhook
        System.out.println("Starting PayHook...");
        PayHook.init(
                "Test-Brand-Name",
                GeneralTest.dbUrl,
                GeneralTest.dbUsername,
                GeneralTest.dbPassword,
                true,
                "https://my-shop.com/payment/success",
                "https://my-shop.com/payment/cancel");

        // Init processors
        PayHook.initStripe(stripeSecretKey, stripeWebhookUrl);
        PayHook.initPayPal(paypalClientId, paypalClientSecret, paypalWebhookUrl);

        // Delete old webhook endpoints that have ngrok.io in their url // STRIPE
        Map<String, Object> params = new HashMap<>();
        params.put("limit", "100");
        for (WebhookEndpoint webhook :
                WebhookEndpoint.list(params).getData()) {
            if (!webhook.getUrl().equals(stripeWebhookUrl) && webhook.getUrl().contains("ngrok.io")) {
                webhook.delete();
            }
        }

        // Delete old webhook endpoints that have ngrok.io in their url // PAYPAL
        for (JsonElement el : PayHook.myPayPal.getWebhooks()) {
            JsonObject webhook = el.getAsJsonObject();
            String url = webhook.get("url").getAsString();
            if(!url.equals(paypalWebhookUrl) && url.contains("ngrok.io")){
                String id = webhook.get("id").getAsString();
                PayHook.myPayPal.deleteWebhook(id);
            }
        }
        System.out.println("Payment processors initialised with webhooks above.");


        // Create/Update products
        pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE);
        pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.MONTHLY);
        System.out.println("Created/Updated products.");
        System.out.println("OK!");

        //oneTimePayment(PaymentProcessor.PAYPAL);
        oneTimePayment(PaymentProcessor.STRIPE);
    }

    private void oneTimePayment(PaymentProcessor paymentProcessor) throws Exception {
        System.out.println("Test buying "+pCoolCookie.name+" over "+paymentProcessor+", waiting for user authorization...");
        Payment payment = PayHook.createPayment("testUser", pCoolCookie, paymentProcessor);
        AtomicBoolean isAuthorized = new AtomicBoolean(false);
        PayHook.onPaymentAuthorized.addAction((action, event) -> {
            System.out.println("Received authorized payment for "+event.payment.productName+" "+new Converter().toMoneyString(pCoolCookie));
            isAuthorized.set(true);
        }, Throwable::printStackTrace);
        Desktop.getDesktop().browse(URI.create(payment.url));
        while (!isAuthorized.get()) Thread.sleep(100);
    }

    private void doPayPalWebhookEvent(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        try {
            response.status(200); // Directly set status code
            PayHook.receiveWebhookEvent(
                    PaymentProcessor.PAYPAL,
                    getHeadersAsMap(request),
                    request.readBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            // TODO handle exception
        }
    }

    private void doStripeWebhookEvent(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        try {
            response.status(200); // Directly set status code
            PayHook.receiveWebhookEvent(
                    PaymentProcessor.STRIPE,
                    getHeadersAsMap(request),
                    request.readBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            // TODO handle exception
        }
    }

    // Simple helper method to help you extract the headers from HttpServletRequest object.
    private Map<String, String> getHeadersAsMap(MuRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        request.headers().forEach(e -> {
            map.put(e.getKey(), e.getValue());
        });
        return map;
    }
}
