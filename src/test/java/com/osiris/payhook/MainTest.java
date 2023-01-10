package com.osiris.payhook;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.dyml.Yaml;
import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.jsqlgen.payhook.Product;
import com.osiris.payhook.utils.Converter;
import com.stripe.model.WebhookEndpoint;
import io.muserver.*;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainTest {
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
     * Note that this will delete old sandbox webhooks that contain ngrok.io in their url. <br>
     */
    public static void main(String[] args) throws Exception {

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

        // Init web-server to listen for webhook events, http://localhost:80/
        System.out.println("Starting web-server...");
        MuServer server = MuServerBuilder.httpServer()
                .withHttpPort(80)
                .addHandler(Method.GET, "/", (request, response, pathParams) -> {
                    response.write("Currently running from " + MainTest.class);
                })
                .addHandler(Method.POST, "/paypal-hook", MainTest::doPayPalWebhookEvent)
                .addHandler(Method.POST, "/stripe-hook", MainTest::doStripeWebhookEvent)
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
        System.out.println("Url: " + dbUrl);
        System.out.println("OK!");

        // Initialise payhook
        System.out.println("Starting PayHook...");
        PayHook.init(
                "Test-Brand-Name",
                MainTest.dbUrl,
                dbServer.getName(),
                MainTest.dbUsername,
                MainTest.dbPassword,
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
        for (JsonElement el : PayHook.paypalUtils.getWebhooks()) {
            JsonObject webhook = el.getAsJsonObject();
            String url = webhook.get("url").getAsString();
            if (!url.equals(paypalWebhookUrl) && url.contains("ngrok.io")) {
                String id = webhook.get("id").getAsString();
                PayHook.paypalUtils.deleteWebhook(id);
            }
        }
        System.out.println("Payment processors initialised with webhooks above.");


        // Create/Update products
        pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Interval.NONE);
        pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Interval.MONTHLY);
        System.out.println("Created/Updated products.");
        System.out.println("OK!");


        /*
        // The below doesn't work, instead cancel the subscription in the next
        // run of the program
        List<Subscription> listSubscriptionsToCancel = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Subscription subscription : listSubscriptionsToCancel) {
                try {
                    subscription.cancel(); // Cancel all subscriptions created in this session on exit
                    System.out.println("Waiting for cancel confirmation of subscription: " + subscription.toPrintString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));
         */

        // Register event listeners
        PayHook.onPaymentAuthorized.addAction((action, payment) -> {
            System.out.println("(BACKEND) AUTHORIZED PAYMENT: " + payment.toPrintString());
            //if (payment.isRecurring()) {
            //    listSubscriptionsToCancel.add(new Subscription(payment));
            //}
        }, Exception::printStackTrace);
        PayHook.onPaymentCancelled.addAction((action, payment) -> {
            System.out.println("(BACKEND) CANCELLED PAYMENT: " + payment.toPrintString());
        }, Exception::printStackTrace);
        PayHook.onPaymentRefunded.addAction((action, payment) -> {
            System.out.println("(BACKEND) REFUNDED PAYMENT: " + payment.toPrintString());
        }, Exception::printStackTrace);
        PayHook.onPaymentExpired.addAction((action, payment) -> {
            System.out.println("(BACKEND) EXPIRED PAYMENT: " + payment.toPrintString());
        }, Exception::printStackTrace);

        // Check for active subscriptions and cancel them:
        for (Subscription sub : Subscription.getNotCancelled()) {
            try{
                sub.cancel();
                System.out.println("Cancelled active subscription: "+ sub.toPrintString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Test payments
        System.out.println("===========================================================================================");
        System.out.println("===========================================================================================");
        System.out.println("===========================================================================================");
        printHelp();
        System.out.println("===========================================================================================");
        System.out.println("Listening for user input.");
        System.out.println("You can test payments (buy products) for example. Enter 'help' to list all commands.");
        System.out.println("Note that receiving PayPal webhook events can take up to a few minutes.");
        System.out.println("This is probably due to the test environment.");

        while (true) {
            String command = new Scanner(System.in).nextLine().trim();
            try {
                if (command.equals("help")) {
                    printHelp();
                } else if (command.equals("buy cool-cookie paypal"))
                    waitForPayment(pCoolCookie, PaymentProcessor.PAYPAL);
                else if (command.equals("buy cool-cookie stripe"))
                    waitForPayment(pCoolCookie, PaymentProcessor.STRIPE);
                else if (command.equals("buy cool-subscription paypal"))
                    waitForPayment(pCoolSubscription, PaymentProcessor.PAYPAL);
                else if (command.equals("buy cool-subscription stripe"))
                    waitForPayment(pCoolSubscription, PaymentProcessor.STRIPE);
                else if (command.startsWith("cancel ")) {
                    String paymentId = command.replace("cancel ", "").trim();
                    List<Payment> payments = Payment.whereId().is(Integer.parseInt(paymentId)).get();
                    if (payments.isEmpty())
                        System.err.println("Payment with id '" + paymentId + "' not found in database!");
                    else {
                        PayHook.cancelPayment(payments.get(0));
                        System.out.println("Cancelled payment!");
                    }
                } else if (command.startsWith("refund ")) {
                    String paymentId = command.replace("refund ", "").trim();
                    List<Payment> payments = Payment.whereId().is(Integer.parseInt(paymentId)).get();
                    if (payments.isEmpty())
                        System.err.println("Payment with id '" + paymentId + "' not found in database!");
                    else {
                        PayHook.refundPayments(payments.get(0));
                        System.out.println("Refunded payment!");
                    }
                } else if (command.equals("print payments")) {
                    List<Payment> payments = Payment.get();
                    System.out.println("Showing " + payments.size() + " payments:");
                    for (Payment payment : payments) {
                        System.out.println(payment.toPrintString());
                    }
                } else if (command.equals("print products")) {
                    List<Product> products = Product.get();
                    System.out.println("Showing " + products.size() + " products:");
                    for (Product product : products) {
                        System.out.println(product.toPrintString());
                    }
                } else if (command.equals("print subscriptions")) {
                    for (Subscription sub : Subscription.paymentsToSubscriptions(Payment.get())) {
                        System.out.println(sub.toPrintString());
                    }
                } else if (command.equals("print active subscriptions")) {
                    for (Subscription sub : Subscription.getNotCancelled()) {
                        System.out.println(sub.toPrintString());
                    }
                } else if (command.startsWith("delete payment")) {
                    int id = Integer.parseInt(command.replace("delete payment ", "").trim());
                    Payment.whereId().is(id).remove();
                } else if (command.startsWith("delete product")) {
                    int id = Integer.parseInt(command.replace("delete product ", "").trim());
                    Product.whereId().is(id).remove();
                } else
                    System.err.println("Unknown command '" + command + "', please enter a valid one.");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Something went wrong during command execution. See details above.");
            }
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println();
        System.out.println("buy cool-cookie paypal");
        System.out.println("buy cool-cookie stripe");
        System.out.println("buy cool-subscription paypal");
        System.out.println("buy cool-subscription stripe");
        System.out.println();
        System.out.println("refund <payment-id>");
        System.out.println("cancel <payment-id>");
        System.out.println();
        System.out.println("print payments");
        System.out.println("print products");
        System.out.println("print subscriptions");
        System.out.println("print active subscriptions");
        System.out.println();
        System.out.println("delete payment <id>");
        System.out.println("delete product <id>");
    }

    private static void waitForPayment(Product product, PaymentProcessor paymentProcessor) throws Exception {
        AtomicBoolean isAuthorized = new AtomicBoolean(false);
        System.out.println("Buying " + product.name + " over " + paymentProcessor + ".");
        Payment payment = PayHook.expectPayment("testUser", product, paymentProcessor,
                authorizedPayment -> {
            isAuthorized.set(true);
                    // OPTIONAL --
                    // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                    // Code that does backend, aka important stuff does not belong here!
                    // Gets executed when the payment was authorized.
                    System.out.println("Received authorized payment for " +
                            authorizedPayment.productName + " " + new Converter().toMoneyString(authorizedPayment.currency, authorizedPayment.charge));
                },
                cancelledPayment -> {
                    // OPTIONAL --
                    // Insert ONLY additional UI code here (make sure to have access to the UI thread).
                    // Code that does backend, aka important stuff does not belong here!
                    // Gets executed when the payment was cancelled.
                    System.out.println("Cancelled payment for " +
                            cancelledPayment.productName + " " + new Converter().toMoneyString(cancelledPayment.currency, cancelledPayment.charge));
                });

        System.out.println("Authorize payment here: " + payment.url);
        System.out.println("Waiting for you to authorize the payment...");
        if (Desktop.isDesktopSupported())
            Desktop.getDesktop().browse(URI.create(payment.url));
        while (!isAuthorized.get()) Thread.sleep(100);
    }

    private static void doPayPalWebhookEvent(MuRequest request, MuResponse response, Map<String, String> pathParams) {
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

    private static void doStripeWebhookEvent(MuRequest request, MuResponse response, Map<String, String> pathParams) {
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
    private static Map<String, String> getHeadersAsMap(MuRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        request.headers().forEach(e -> {
            map.put(e.getKey(), e.getValue());
        });
        return map;
    }
}
