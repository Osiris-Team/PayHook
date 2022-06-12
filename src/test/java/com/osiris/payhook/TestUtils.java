package com.osiris.payhook;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Create test-credentials.txt inside the project root directory. <br>
 * First line is the stripe secret key. <br>
 */
public class TestUtils {
    public static SQLTestServer server;
    public static String dbUrl;
    public static String dbUsername = "root";
    public static String dbPassword = "";
    public static String stripeSecretKey = null;
    public static boolean isInit = false;

    /**
     * Initialises PayHook test environment if not already done.
     */
    @BeforeAll
    public static void init() {
        try{
            if(isInit) return;

            // Init test database
            server = SQLTestServer.buildAndRun("testDB");
            dbUrl = server.getUrl();

            // Fetch test credentials
            File f = new File(System.getProperty("user.dir")+"/test-credentials.txt");
            if (!f.exists()) throw new FileNotFoundException("Make sure that the credentials file exists at: "+f.getAbsolutePath());
            try(BufferedReader reader = new BufferedReader(new FileReader(f))){
                stripeSecretKey = reader.readLine();
            }
            if(stripeSecretKey==null) throw new Exception("First line of test-credentials.txt must be the stripe secret key!");

            // Initialise payhook
            PayHook.init(
                    "Test-Brand-Name",
                    TestUtils.dbUrl,
                    TestUtils.dbUsername,
                    TestUtils.dbPassword,
                    true);

            // Setup ngrok to tunnel traffic from public ip the current locally running service/app
            // Open a HTTP tunnel on the default port 80
            // <Tunnel: "http://<public_sub>.ngrok.io" -> "http://localhost:80">
            final NgrokClient ngrokClient = new NgrokClient.Builder().build();
            final Tunnel httpTunnel = ngrokClient.connect();
            // TODO test and implement the above

            PayHook.initStripe(TestUtils.stripeSecretKey, webhookUrl);

            pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE, 0);
            pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.DAYS_30, 0);
            isInit = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
