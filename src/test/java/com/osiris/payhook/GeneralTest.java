package com.osiris.payhook;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.osiris.dyml.Yaml;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * Create test-credentials.yml inside the project root directory. <br>
 * First line is the stripe secret key. <br>
 */
public class GeneralTest {
    public static SQLTestServer dbServer;
    public static String dbUrl;
    public static String dbUsername = "root";
    public static String dbPassword = "";
    public static String stripeSecretKey = null;
    private static Product pCoolCookie;
    private static Product pCoolSubscription;

    /**
     * Initialises PayHook test environment if not already done.
     */
    @BeforeAll
    @Test
    public static void run() throws Exception {
        // Init test database without password
        dbServer = SQLTestServer.buildAndRun("testDB");
        dbUrl = dbServer.getUrl();

        // Fetch test credentials
        Yaml yaml = new Yaml(System.getProperty("user.dir")+"/test-credentials.yml");
        yaml.load();
        String stripeSecretKey = yaml.put("stripe secret key").asString();
        String paypalClientId = yaml.put("paypal client id").asString();
        String paypalClientSecret = yaml.put("paypal client secret").asString();
        yaml.save();

        // Test credentials check
        Objects.requireNonNull(stripeSecretKey);
        Objects.requireNonNull(paypalClientId);
        Objects.requireNonNull(paypalClientSecret);

        // Initialise payhook
        PayHook.init(
                "Test-Brand-Name",
                GeneralTest.dbUrl,
                GeneralTest.dbUsername,
                GeneralTest.dbPassword,
                true);

        // Setup ngrok to tunnel traffic from public ip the current locally running service/app
        // Open a HTTP tunnel on the default port 80
        // <Tunnel: "http://<public_sub>.ngrok.io" -> "http://localhost:80">
        final NgrokClient ngrokClient = new NgrokClient.Builder().build();
        final Tunnel httpTunnel = ngrokClient.connect();
        String baseUrl = httpTunnel.getPublicUrl();
        String stripeWebhookUrl = baseUrl+"/stripe-hook";
        String paypalWebhookUrl = baseUrl+"/paypal-hook";
        System.out.println("Public baseUrl: "+baseUrl);
        System.out.println("Public stripeWebhookUrl: "+stripeWebhookUrl);
        System.out.println("Public paypalWebhookUrl: "+paypalWebhookUrl);

        // Init processors
        PayHook.initStripe(stripeSecretKey, stripeWebhookUrl);
        PayHook.initPayPal(paypalClientId, paypalClientSecret, paypalWebhookUrl);
        System.out.println("Payment processors initialised with webhooks above.");

        // Create products
        pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE, 0);
        pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.DAYS_30, 0);
    }

}
