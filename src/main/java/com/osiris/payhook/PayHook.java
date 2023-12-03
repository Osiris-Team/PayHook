package com.osiris.payhook;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.jlib.json.exceptions.HttpErrorException;
import com.osiris.jlib.json.exceptions.WrongJsonTypeException;
import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.jsqlgen.payhook.PendingPaymentCancel;
import com.osiris.jsqlgen.payhook.Product;
import com.osiris.payhook.exceptions.InvalidChangeException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.PayPalPlan;
import com.osiris.payhook.paypal.PayPalUtils;
import com.osiris.payhook.paypal.PaypalWebhookEvent;
import com.osiris.payhook.stripe.UtilsStripe;
import com.osiris.payhook.utils.Converter;
import com.paypal.base.codec.binary.Base64;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.WebhookEndpoint;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Still work in progress. <br>
 * Release planned in v3.0 <br>
 */
public final class PayHook {

    // The cleaner thread is needed
    // to remove the added actions that may not get executed once.
    // Remove them after 6-7 hours.
    // Note that these actions must have their obj set to their creation timestamp in millis.
    // Otherwise, they cannot be removed and will ultimately result in out of memory exception.

    /**
     * Actions for this event are executed, when a payment is created
     * via {@link PayHook#expectPayments(String, List, PaymentProcessor, Consumer, Consumer)}.
     */
    public static final com.osiris.events.Event<Payment> onPaymentCreated = new com.osiris.events.Event<Payment>().initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6 hours
    }, Exception::printStackTrace);
    /**
     * Actions for this event are executed,
     * when a valid payment (authorized payment) was received via a webhook notification. <br>
     * This happens for example when the user goes to {@link Payment#url} and completes the steps, <br>
     * or when an automatic payment happens on an already running subscription.
     */
    public static final com.osiris.events.Event<Payment> onPaymentAuthorized = new com.osiris.events.Event<Payment>().initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6 hours
    }, Exception::printStackTrace);
    /**
     * Actions for this event are executed when a webhook event was received that the subscription was cancelled. <br>
     * Note that if a subscription was cancelled by you or the customer, but there is still time left this event is executed twice. <br>
     * First when the person issued the cancellation (here {@link Subscription#getMillisLeftWithPuffer()} will be bigger than 1). <br>
     * Second when the subscriptions time runs out and {@link Subscription#getMillisLeftWithPuffer()} is smaller than 1.
     */
    public static final com.osiris.events.Event<Payment> onPaymentCancelled = new com.osiris.events.Event<Payment>().initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6 hours
    }, Exception::printStackTrace);

    /**
     * Actions for this event are executed,
     * when a payment was refunded via {@link #refundPayments(List, Consumer)} programmatically or when
     * a refund webhook notification was received. <br>
     */
    public static final com.osiris.events.Event<Payment> onPaymentRefunded = new com.osiris.events.Event<Payment>().initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6 hours
    }, Exception::printStackTrace);

    /**
     * Actions for this event are executed,
     * when a payment was not paid in time so when {@link Payment#timestampExpires} is greater than the current time
     * and {@link Payment#timestampAuthorized} and {@link Payment#timestampCancelled} are null/0. <br>
     * Note that this event can be executed multiple times for a single payment if you don't handle it by, for example
     * setting the {@link Payment#timestampCancelled} to the current time. <br>
     * Also remember that in the case of a subscription,
     * {@link Subscription#getMillisLeftWithPuffer()} will be smaller than 1.<br>
     */
    public static final com.osiris.events.Event<Payment> onPaymentExpired = new com.osiris.events.Event<Payment>().initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6 hours
    }, Exception::printStackTrace);

    /**
     * How long is a stripe payment url valid?
     * It is valid as long as the stripe session is valid.
     * The default is 24h hours.
     *
     * @see <a href="https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-expires_at">Stripe docs</a>
     */
    public static final long stripeUrlTimeoutMs = 86400000;
    private static final boolean isBraintreeWebhookActive = false;
    private static final List<String> paypalWebhookEventTypes = Arrays.asList(
            "BILLING.SUBSCRIPTION.CANCELLED", // Should fire on a subscription cancel
            "PAYMENT.SALE.COMPLETED", // Should fire on a subscription payment
            "CHECKOUT.ORDER.APPROVED",  // Should fire on a checkout payment
            "PAYMENT.CAPTURE.REFUNDED",// Should fire on refunded checkout
            "PAYMENT.SALE.REFUNDED"); // Should fire on refunded subscription payment
    public static String brandName;
    public static boolean isInitialised = false;
    public static boolean isSandbox = false;
    /**
     * Redirect the user to this url after a successful checkout. <br>
     * This value should be final, not null and not contain
     * any session related information or any personal information about the buyer. <br>
     * Example: https://my-shop/payment/success
     */
    public static String successUrl;
    /**
     * Redirect the user to this url after an aborted checkout. <br>
     * This value should be final, not null and not contain
     * any session related information or any personal information about the buyer. <br>
     * Example: https://my-shop/payment/cancel
     */
    public static String cancelUrl;
    // Stripe specific:
    public static Stripe stripe;
    /**
     * payment_intent.succeeded: Occurs whenever payment is authorized/successful. <br>
     * invoice.created: Payment on a subscription created, but not finalized. <br>
     * invoice.paid: Payment on a subscription. <br>
     * customer.subscription.deleted: Occurs whenever a customer’s subscription ends. <br>
     *
     * @see <a href="https://stripe.com/docs/api/webhook_endpoints/update">Stripe docs</a>
     */
    public static List<String> stripeWebhookEventTypes = Arrays.asList("checkout.session.completed", "invoice.created",
            "invoice.paid", "customer.subscription.deleted", "charge.refunded");


    // Braintree specific:
    public static BraintreeGateway braintree;
    public static List<String> braintreeWebhookEventTypes = Arrays.asList(); // TODO
    // PayPal specific:
    public static PayPalUtils paypalUtils;
    public static APIContext paypalV1;
    public static PayPalHttpClient paypalV2;
    /**
     * How long is a PayPal payment url valid? 3 hours. <br>
     * From the docs: Once redirected, the API caller has 3 hours for the payer to approve the order and either authorize or capture the order.
     *
     * @see <a href="https://developer.paypal.com/docs/api/orders/v2/#orders-create-response">PayPal docs</a>
     */
    public static long paypalUrlTimeoutMs = 10800000;
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread stripeThread;
    private static boolean isStripeWebhookActive = false;
    /**
     * Additional time in milliseconds that is given to the user (default 1 day),
     * to pay a recurring payment, to prevent direct expiration of the subscription.
     * Example: Subscription runs for 30 days and would expire directly at the end of that day
     * resulting in a {@link #onPaymentCancelled}, if the payment processor takes a little longer
     * to send the payment for the next 30 days. Thus, 1 additional day (default) is given to receive the payment
     * for the next 30 days.
     */
    //TODO NOT NEEDED: REMOVE WHEN DONE WITH DATABASE: public static long recurringPaymentAddedTimeMs;
    private static Thread commandLineThread;
    private static Thread paymentsCheckerThread;
    /**
     * A secret key specific to the webhook that is used to validate it.
     */
    private static String stripeWebhookSecret;
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread braintreeThread;
    private static String paypalClientId;
    private static String paypalClientSecret;
    private static String paypalBase64EncodedCredentials;
    private static String paypalWebhookId;
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread paypalThread;
    private static boolean isPayPalWebhookActive = false;

    /**
     * Always null, except when doing init of database.
     */
    public static String databaseUrl = null;
    public static String databaseRawUrl = null;
    /**
     * Always null, except when doing init of database.
     */
    public static String databaseName = null;
    /**
     * Always null, except when doing init of database.
     */
    public static String databaseUsername = null;
    /**
     * Always null, except when doing init of database.
     */
    public static String databasePassword = null;

    /**
     * If {@link #isSandbox} = true then the "payhook_sandbox" database will get created/used, otherwise
     * the default "payhook" database.
     * Remember to set your {@link PaymentProcessor} credentials, before
     * creating/updating any {@link Product}s. <br>
     *
     * @param brandName
     * @param databaseUrl      Example: "jdbc:mysql://localhost:3306/db_name?serverTimezone=Europe/Rome".
     * @param databaseUsername Example: "root".
     * @param databasePassword Example: "".
     * @param isSandbox        Set false in production, set true when testing.
     * @param successUrl       See {@link #successUrl}.
     * @param cancelUrl        See {@link #cancelUrl}.
     * @throws SQLException     When the database name contains "test" when running in production or another error happens during database initialisation.
     * @throws RuntimeException when there is an error in the thread, which checks for missed payments in a regular interval.
     */
    public static synchronized void init(String brandName, String databaseUrl, String databaseName,
                                         String databaseUsername, String databasePassword, boolean isSandbox,
                                         String successUrl, String cancelUrl) throws SQLException {
        if (isInitialised) return;
        PayHook.brandName = Objects.requireNonNull(brandName);
        PayHook.isSandbox = isSandbox;
        PayHook.successUrl = Objects.requireNonNull(successUrl);
        PayHook.cancelUrl = Objects.requireNonNull(cancelUrl);
        Objects.requireNonNull(databaseUrl);
        if (!isSandbox && (databaseUrl.contains("sandbox") || databaseUrl.contains("test")))
            throw new SQLException("You are NOT running in sandbox mode, thus your database-url/name CANNOT contain 'sandbox' or 'test'!");
        if (isSandbox && (!databaseUrl.contains("sandbox") && !databaseUrl.contains("test")))
            throw new SQLException("You are running in sandbox mode, thus your database-url/name must contain 'sandbox' or 'test'!");
        PayHook.databaseUrl = databaseUrl;
        PayHook.databaseRawUrl = getRawDbUrlFrom(databaseUrl);
        PayHook.databaseName = Objects.requireNonNull(databaseName);
        PayHook.databaseUsername = Objects.requireNonNull(databaseUsername);
        PayHook.databasePassword = Objects.requireNonNull(databasePassword);
        com.osiris.jsqlgen.payhook.Database.create();
        // Reset values since they aren't needed anymore
        PayHook.databaseUrl = null;
        PayHook.databaseRawUrl = null;
        PayHook.databaseName = null;
        PayHook.databaseUsername = null;
        PayHook.databasePassword = null;

        paymentsCheckerThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(3600000); // 1h
                    long now = System.currentTimeMillis();

                    // Pending payments that haven't been authorized within the time limit.
                    for (Payment pendingPayment : Payment.getPendingPayments()) {
                        if (now > pendingPayment.timestampExpires) {
                            onPaymentExpired.execute(pendingPayment);
                        }
                    }

                    // Execute expired payment event for subscriptions that are active
                    // but where the last payment exceeds the time limit + puffer.
                    for (Subscription sub : Subscription.getNotCancelled()) {
                        if (sub.getMillisLeftWithPuffer() < 1) {
                            onPaymentExpired.execute(sub.getLastPayment());
                        }
                    }

                    // Execute PendingPaymentCancel
                    for (PendingPaymentCancel pendingCancel : PendingPaymentCancel.get()) {
                        long msLeft = now - pendingCancel.timestampCancel;
                        if(msLeft < 1){
                            PendingPaymentCancel.remove(pendingCancel);
                            onPaymentCancelled.execute(Payment.get(pendingCancel.paymentId));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        paymentsCheckerThread.start();

        isInitialised = true;
    }

    /**
     * Gets the raw database url without database name. <br>
     * Before: "jdbc:mysql://localhost/my_database" <br>
     * After: "jdbc:mysql://localhost" <br>
     */
    private static String getRawDbUrlFrom(String databaseUrl) {
        int index = 0;
        int count = 0;
        for (int i = 0; i < databaseUrl.length(); i++) {
            char c = databaseUrl.charAt(i);
            if(c == '/'){
                index = i;
                count++;
            }
            if(count == 3) break;
        }
        if(count != 3) return databaseUrl; // Means there is less than 3 "/", thus may already be raw url, or totally wrong url
        return databaseUrl.substring(0, index);
    }

    public static void initCommandLineTool() {
        if (commandLineThread != null) commandLineThread.interrupt();
        commandLineThread = new Thread(() -> {
            PrintStream out = System.out;
            Scanner scanner = new Scanner(System.in);
            out.println("Initialised PayHooks' command line tool. To exit it enter 'exit', for a list of commands enter 'help'.");
            boolean exit = false;
            String command = null;
            while (!exit) {
                command = scanner.nextLine();
                try {
                    if (command.equals("exit")) {
                        exit = true;
                    } else if (command.equals("help") || command.equals("h")) {
                        out.println("Available commands:");
                        out.println("products delete <id> | Removes the product with the given id from the local database.");
                        // TODO add commands like:
                        // payments <days> // Prints all received payments from the last <days> (if not provided 30 days is the default)
                    } else if (command.startsWith("products delete")) {
                        int id = Integer.parseInt(command.replaceFirst("products delete ", "").trim());
                        Product.remove(Product.get(id)); //TODO delete everywhere
                    } else {
                        out.println("Unknown command. Enter 'help' or 'h' for a list of all commands.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        commandLineThread.start();
    }

    /**
     * Sets the Braintree credentials and initialises its APIs/SDKs. <br>
     * <p style="color:red;"> Important: </p>You must create the webhook with the allowed types yourself. See Braintrees' webhook docs
     * <a href="https://developer.paypal.com/braintree/docs/guides/webhooks/overview">here</a> for details. <br>
     *
     * @param merchantId See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param publicKey  See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param privateKey See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param webhookUrl Something like this: "https://my-shop.com/braintree-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String)} when receiving a webhook notification/event
     *                   on that url.
     */
    public static void initBraintree(String merchantId, String publicKey, String privateKey, String webhookUrl) throws IOException, HttpErrorException {
        if (isSandbox) {
            PayHook.braintree = new BraintreeGateway(
                    Environment.SANDBOX,
                    merchantId,
                    publicKey,
                    privateKey
            );
        } else {
            PayHook.braintree = new BraintreeGateway(
                    Environment.PRODUCTION,
                    merchantId,
                    publicKey,
                    privateKey
            );
        }
        /*
        TODO
        boolean containsWebhookUrl = false;
        for (JsonElement e :
                paypalREST.getWebhooks()) {
            JsonObject webhook = e.getAsJsonObject();
            String url = webhook.get("url").getAsString();
            if(url.equals(webhookUrl)){
                paypalWebhookId = webhook.get("id").getAsString();
                containsWebhookUrl = true;
                break;
            }
        }
        if(!containsWebhookUrl) paypalREST.createWebhook(webhookUrl, braintreeWebhookEventTypes);*/
    }

    /**
     * Sets the Stripe credentials and initialises its APIs/SDKs. <br>
     * Also creates/registers the required webhook if needed. <br>
     *
     * @param secretKey  See Stripes' docs <a href="https://stripe.com/docs/keys">here</a> for details.
     * @param webhookUrl TODO If null, webhooks will not be used, instead regular REST-API requests will be made.
     *                   Something like this: "https://my-shop.com/stripe-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String)} when receiving a webhook notification/event
     *                   on that url.
     */
    public static void initStripe(String secretKey, String webhookUrl) throws StripeException {
        Objects.requireNonNull(secretKey);
        Objects.requireNonNull(webhookUrl);
        Stripe.apiKey = secretKey;
        if (webhookUrl == null) {
            //TODO
        } else {
            isStripeWebhookActive = true;
            Map<String, Object> params = new HashMap<>();
            params.put("limit", "100");
            WebhookEndpoint targetWebhook = null;
            for (WebhookEndpoint webhook :
                    WebhookEndpoint.list(params).getData()) {
                if (webhook.getUrl().equals(webhookUrl)) {
                    targetWebhook = webhook;
                    break;
                }
            }
            if (targetWebhook == null) {
                Map<String, Object> params2 = new HashMap<>();
                params2.put("url", webhookUrl);
                params2.put("enabled_events", stripeWebhookEventTypes);
                targetWebhook = WebhookEndpoint.create(params2);
                stripeWebhookSecret = targetWebhook.getSecret();
                List<com.osiris.jsqlgen.payhook.WebhookEndpoint> webhookEndpoints = com.osiris.jsqlgen.payhook.WebhookEndpoint.whereUrl().is(webhookUrl).get();
                for (com.osiris.jsqlgen.payhook.WebhookEndpoint webhookEndpoint : webhookEndpoints) {
                    com.osiris.jsqlgen.payhook.WebhookEndpoint.remove(webhookEndpoint);
                }
                com.osiris.jsqlgen.payhook.WebhookEndpoint.createAndAdd(webhookUrl, stripeWebhookSecret);
            }
            stripeWebhookSecret = com.osiris.jsqlgen.payhook.WebhookEndpoint.
                    whereUrl().is(targetWebhook.getUrl()).get().get(0).stripeWebhookSecret;
        }
        Objects.requireNonNull(stripeWebhookSecret);
    }

    /**
     * Sets the PayPal credentials and initialises its APIs/SDKs. <br>
     * Also creates/registers the required Webhook if needed. <br>
     *
     * @param clientId     See PayPals' docs <a href="https://developer.paypal.com/api/rest/#link-getcredentials">here</a> for details.
     * @param clientSecret See PayPals' docs <a href="https://developer.paypal.com/api/rest/#link-getcredentials">here</a> for details.
     * @param webhookUrl   TODO If null, webhooks will not be used, instead regular REST-API requests will be made.
     *                     Something like this: "https://my-shop.com/paypal-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                     run {@link #receiveWebhookEvent(PaymentProcessor, Map, String)} when receiving a webhook notification/event
     *                     on that url.
     */
    public static void initPayPal(String clientId, String clientSecret, String webhookUrl) throws IOException, HttpErrorException, WrongJsonTypeException {
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(clientSecret);
        Objects.requireNonNull(webhookUrl);
        PayHook.paypalClientId = clientId;
        PayHook.paypalClientSecret = clientSecret;

        if (isSandbox) {
            paypalUtils = new PayPalUtils(clientId, clientSecret, PayPalUtils.Mode.SANDBOX);
            paypalV1 = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2 = new PayPalHttpClient(new PayPalEnvironment.Sandbox(clientId, clientSecret));
        } else {
            paypalUtils = new PayPalUtils(clientId, clientSecret, PayPalUtils.Mode.LIVE);
            paypalV1 = new APIContext(clientId, clientSecret, "live");
            paypalV2 = new PayPalHttpClient(new PayPalEnvironment.Live(clientId, clientSecret));
        }
        paypalBase64EncodedCredentials = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
        if (webhookUrl == null) {
            if (paypalThread != null) paypalThread.interrupt();
            // TODO start thread, that checks subscription payments every 12 hours
            paypalThread = new Thread(() -> {

            });
            paypalThread.start();
        } else {
            isPayPalWebhookActive = true;
            boolean containsWebhookUrl = false;
            for (JsonElement e :
                    paypalUtils.getWebhooks()) {
                JsonObject webhook = e.getAsJsonObject();
                String url = webhook.get("url").getAsString();
                if (url.equals(webhookUrl)) {
                    paypalWebhookId = webhook.get("id").getAsString();
                    containsWebhookUrl = true;
                    break;
                }
            }
            if (!containsWebhookUrl) {
                paypalUtils.createWebhook(webhookUrl, paypalWebhookEventTypes);
                for (JsonElement e :
                        paypalUtils.getWebhooks()) {
                    JsonObject webhook = e.getAsJsonObject();
                    String url = webhook.get("url").getAsString();
                    if (url.equals(webhookUrl)) {
                        paypalWebhookId = webhook.get("id").getAsString();
                        break;
                    }
                }
            }
        }
        Objects.requireNonNull(paypalWebhookId);
    }

    /**
     * Call this method after setting the credentials for your payment processors. <br>
     * If the provided id doesn't exist in the database, the product gets created/inserted. <br>
     * If the provided id exists in the database and the new provided values differ from the values in the database, it gets updated. <br>
     * The above also happens for the {@link Product}s saved on the databases of the payment processors. <br>
     *
     * @param id               The unique identifier of this product.
     * @param charge           E.g., 100 cents to charge $1.00 or 100 to charge ¥100, a zero-decimal currency.
     *                         The amount value supports up to eight digits (e.g., a value of 99999999 for a USD charge of $999,999.99).
     * @param currency         Three-letter <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO currency code</a>,
     *                         in lowercase. Must be a <a href="https://stripe.com/docs/currencies">supported currency</a>.
     * @param name             The name of the product.
     * @param description      The products' description.
     * @param paymentIntervall The payment intervall in days. Only relevant for recurring payments. See {@link Payment.Interval} for useful defaults.
     * @throws InvalidChangeException if you tried to change the payment type of the product.
     */
    public static Product putProduct(int id, long charge,
                                     String currency, String name, String description,
                                     int paymentIntervall) throws Exception {
        Converter converter = new Converter();
        Product product = null;
        try {
            product = Product.get(id);
        } catch (Exception ignored) {
        }
        if (product == null) { // Product not existing yet
            product = new Product(id, charge, currency, name, description, paymentIntervall);

            if (paypalUtils != null) { // Create paypal product
                JsonObject response = paypalUtils.createProduct(product);
                product.paypalProductId = response.get("id").getAsString();
                Objects.requireNonNull(product.paypalProductId);
                if (product.isRecurring()) {
                    PayPalPlan plan = paypalUtils.createPlan(product, true);
                    product.paypalPlanId = plan.getPlanId();
                    Objects.requireNonNull(product.paypalPlanId);
                }
            }

            if (braintree != null) ; // TODO

            if (Stripe.apiKey != null) { // Create stripe product
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.create(converter.toStripeProduct(product));
                product.stripeProductId = stripeProduct.getId();
                Objects.requireNonNull(product.stripeProductId);
                com.stripe.model.Price stripePrice = com.stripe.model.Price.create(converter.toStripePrice(product));
                product.stripePriceId = stripePrice.getId();
                Objects.requireNonNull(product.stripePriceId);
            }

            Product.add(product);
        }

        Product newProduct = new Product(id, charge, currency, name, description, paymentIntervall);
        if (newProduct.id != product.id || newProduct.charge != product.charge || !newProduct.currency.equals(product.currency) ||
                !newProduct.name.equals(product.name) || !newProduct.description.equals(product.description) || newProduct.paymentInterval != product.paymentInterval) {
            product.id = id;
            product.charge = charge;
            product.currency = currency;
            product.name = name;
            product.description = description;
            product.paymentInterval = paymentIntervall;

            if (paypalUtils != null) {
                paypalUtils.updateProduct(product);
                if (product.isRecurring()) {
                    com.paypal.api.payments.Plan plan = com.paypal.api.payments.Plan.get(paypalV1, product.paypalPlanId);
                    plan.update(paypalV1, converter.toPayPalPlanPatch(product)); // TODO
                }
            }
            if (Stripe.apiKey != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.retrieve(product.stripeProductId);
                stripeProduct.update(converter.toStripeProduct(product));
                // Is there a price change?
                if(newProduct.charge != product.charge){
                    // Sadly prices cannot be updated in stripe
                    // thus the old one must be deactivated first (can't delete it either)
                    com.stripe.model.Price oldPrice = com.stripe.model.Price.retrieve(product.stripePriceId);
                    // Deactivate old
                    Map<String, Object> params = new HashMap<>();
                    params.put("active", false);
                    oldPrice.update(params);
                    // Activate/Create new
                    com.stripe.model.Price.create(converter.toStripePrice(newProduct));
                }
            }

            Product.update(product);
        }
        return product;
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     *
     * @see #expectPayments(String, List, PaymentProcessor, Consumer, Consumer)
     */
    public static Payment expectPayment(String userId, Product product, PaymentProcessor paymentProcessor) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return expectPayments(userId, products, paymentProcessor, null, null)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}, with custom quantity. <br>
     *
     * @see #expectPayments(String, List, PaymentProcessor, Consumer, Consumer)
     */
    public static Payment expectPayment(String userId, Product product, int quantity, PaymentProcessor paymentProcessor) throws Exception {
        List<Product> products = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return expectPayments(userId, products, paymentProcessor, null, null)
                .get(0);
    }

    // METHODS WITH AUTHORIZED PAYMENT CONSUMER

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     *
     * @see #expectPayments(String, List, PaymentProcessor, Consumer, Consumer)
     */
    public static Payment expectPayment(String userId, Product product, PaymentProcessor paymentProcessor,
                                        Consumer<Payment> onAuthorizedPayment) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return expectPayments(userId, products, paymentProcessor, onAuthorizedPayment, null)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}, with custom quantity. <br>
     *
     * @see #expectPayments(String, List, PaymentProcessor, Consumer, Consumer)
     */
    public static Payment expectPayment(String userId, Product product, int quantity, PaymentProcessor paymentProcessor,
                                        Consumer<Payment> onAuthorizedPayment) throws Exception {
        List<Product> products = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return expectPayments(userId, products, paymentProcessor, onAuthorizedPayment, null)
                .get(0);
    }

    // METHODS WITH AUTHORIZED & CANCELLED PAYMENT CONSUMER

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     *
     * @see #expectPayments(String, List, PaymentProcessor, Consumer, Consumer)
     */
    public static Payment expectPayment(String userId, Product product, PaymentProcessor paymentProcessor,
                                        Consumer<Payment> onAuthorizedPayment, Consumer<Payment> onCancelledPayment) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return expectPayments(userId, products, paymentProcessor, onAuthorizedPayment, onCancelledPayment)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}, with custom quantity. <br>
     *
     * @see #expectPayments(String, List, PaymentProcessor, Consumer, Consumer)
     */
    public static Payment expectPayment(String userId, Product product, int quantity, PaymentProcessor paymentProcessor,
                                        Consumer<Payment> onAuthorizedPayment, Consumer<Payment> onCancelledPayment) throws Exception {
        List<Product> products = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return expectPayments(userId, products, paymentProcessor, onAuthorizedPayment, onCancelledPayment)
                .get(0);
    }

    /**
     * Creates a new pending {@link Payment} for each provided {@link Product}, which expires in a {@link PaymentProcessor} specific time
     * (see {@link #onPaymentCancelled}).
     * Redirect your user to {@link Payment#url} to complete the payment.
     * Note that {@link Product}s WITHOUT recurring payments get grouped together
     * and can be paid over the same url.
     * You can listen for payment authorization/completion at {@link #onPaymentAuthorized}.
     *
     * @param userId           {@link Payment#userId}
     * @param products         List of {@link Product}s the user wants to buy.
     *                         Cannot be null, empty, or contain products with different currencies.
     *                         If the user wants the same {@link Product} twice for example, simply add it twice to this list.
     * @param paymentProcessor The users' desired {@link PaymentProcessor}.
     */
    public static List<Payment> expectPayments(String userId, List<Product> products, PaymentProcessor paymentProcessor,
                                               Consumer<Payment> onAuthorizedPayment, Consumer<Payment> onCancelledPayment) throws Exception {
        Converter converter = new Converter();
        Objects.requireNonNull(products);
        if (products.size() == 0) throw new Exception("Products array cannot be empty!");

        List<Product> productsNOTrecurring = new ArrayList<>();
        List<Product> productsRecurring = new ArrayList<>();
        for (Product p :
                products) {
            if (paymentProcessor.equals(PaymentProcessor.STRIPE) && !p.isStripeSupported())
                throw new Exception("Product with id '" + p.id + "' does not support " + PaymentProcessor.STRIPE + " because of empty fields in the database!");
            else if (paymentProcessor.equals(PaymentProcessor.PAYPAL) && !p.isPayPalSupported())
                throw new Exception("Product with id '" + p.id + "' does not support " + PaymentProcessor.PAYPAL + " because of empty fields in the database!");
            //TODO ADD PAYMENT PROCESSOR

            if (p.isRecurring()) // Sort
                productsRecurring.add(p);
            else
                productsNOTrecurring.add(p);
        }
        Map<Product, Integer> productsAndQuantity = new HashMap<>();
        List<Product> productsCopy2 = new ArrayList<>(productsNOTrecurring);
        for (Product p : productsNOTrecurring) { // Check
            for (Product p2 : productsCopy2) {
                if (!p2.currency.equals(p.currency)) {
                    throw new Exception("All provided products must have the same currency!");
                }
                if (p.equals(p2)) {
                    productsAndQuantity.merge(p, 1, Integer::sum);
                }
            }
        }

        long now = System.currentTimeMillis();
        List<Payment> payments = new ArrayList<>();
        if (paymentProcessor.equals(PaymentProcessor.STRIPE)) { // STRIPE
            if (productsNOTrecurring.size() > 0) {

                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl);
                for (Product p :
                        productsNOTrecurring) {
                    int quantity = productsAndQuantity.get(p);
                    paramsBuilder.addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) quantity)
                                    .setPrice(p.stripePriceId)
                                    .build());
                    payments.add(Payment.create(userId,
                            (quantity * p.charge), p.currency,
                            p.paymentInterval,
                            null, p.id, p.name, quantity,
                            now, now + stripeUrlTimeoutMs, 0, 0, 0,
                            null, null, null,
                            null, null, null));
                }
                Session session = Session.create(paramsBuilder.build());
                for (Payment payment :
                        payments) {
                    payment.url = session.getUrl();
                    payment.stripeSessionId = session.getId();
                    payment.stripePaymentIntentId = session.getPaymentIntent();
                }
            }
            for (Product product :
                    productsRecurring) {
                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl);
                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity((long) 1)
                                .setPrice(product.stripePriceId)
                                .build());
                Session session = Session.create(paramsBuilder.build());

                Payment payment = Payment.create(userId,
                        product.charge, product.currency,
                        product.paymentInterval,
                        session.getUrl(),
                        product.id, product.name, 1, now, now + stripeUrlTimeoutMs, 0, 0, 0,
                        session.getId(), null, null, //session.getPaymentIntent() and session.getSubscription() always returns null here
                        null, null, null);
                payments.add(payment);
            }
        } else if (paymentProcessor.equals(PaymentProcessor.PAYPAL)) { // PAYPAL

            if (productsNOTrecurring.size() > 0) {
                OrderRequest orderRequest = new OrderRequest();
                orderRequest.checkoutPaymentIntent("CAPTURE");

                ApplicationContext applicationContext = new ApplicationContext().brandName(brandName).landingPage("BILLING")
                        .cancelUrl(cancelUrl).returnUrl(successUrl).userAction("CONTINUE")
                        .shippingPreference("NO_SHIPPING");
                orderRequest.applicationContext(applicationContext);

                String currency = null;
                long priceTotal = 0;
                List<Item> items = new ArrayList<>();
                for (Product product :
                        productsNOTrecurring) {
                    int quantity = productsAndQuantity.get(product);
                    priceTotal += product.charge;
                    items.add(new Item().name(product.name).description(product.description)
                            .unitAmount(new Money().currencyCode(product.currency).value(converter.toPayPalCurrency(product).getValue())).quantity("" + quantity)
                            .category("DIGITAL_GOODS"));
                    payments.add(Payment.create(userId,
                            (quantity * product.charge), product.currency,
                            product.paymentInterval, null,
                            product.id, product.name, quantity, now, now + paypalUrlTimeoutMs, 0, 0, 0,
                            null, null, null,
                            null, null, null));
                }
                currency = payments.get(0).currency;
                PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                        .description(brandName).softDescriptor(brandName)
                        .customId("" + payments.get(0).id) // Set first payment id of the order, as custom id
                        .amountWithBreakdown(new AmountWithBreakdown().currencyCode(currency).value(converter.toPayPalCurrency(currency, priceTotal).getValue())
                                .amountBreakdown(new AmountBreakdown().itemTotal(new Money().currencyCode(currency).value(converter.toPayPalCurrency(currency, priceTotal).getValue()))
                                ))
                        .items(items);
                List<PurchaseUnitRequest> purchaseUnitRequests = new ArrayList<>();
                purchaseUnitRequests.add(purchaseUnitRequest);
                orderRequest.purchaseUnits(purchaseUnitRequests);
                OrdersCreateRequest request = new OrdersCreateRequest();
                request.header("prefer", "return=representation");
                request.requestBody(orderRequest);

                HttpResponse<com.paypal.orders.Order> response = paypalV2.execute(request);
                if (response.statusCode() != 201)
                    throw new PayPalRESTException("Failed to create order! Braintree returned error code '" + response.statusCode() + "'.");
                String payUrl = null;
                for (LinkDescription link : response.result().links()) {
                    if (link.rel().equals("approve")) {
                        payUrl = link.href();
                        break;
                    }
                }
                if (payUrl == null) throw new PayPalRESTException("Failed to determine payUrl!");
                for (Payment payment :
                        payments) {
                    payment.url = payUrl;
                    payment.paypalOrderId = response.result().id();
                }
            }
            for (Product p :
                    productsRecurring) {
                Payment payment = Payment.create(userId,
                        p.charge, p.currency, p.paymentInterval, null,
                        p.id, p.name, 1, now, now + paypalUrlTimeoutMs, 0, 0, 0,
                        null, null, null,
                        null, null, null);
                // Save the paymentId in customId of paypal subscription
                // to find this payment later when receiving payments via the webhook.
                String[] arr = paypalUtils.createSubscription(brandName, p.paypalPlanId, "" + payment.id, successUrl, cancelUrl);
                payment.paypalSubscriptionId = arr[0];
                payment.url = arr[1];
                payments.add(payment);
            }
        } else
            throw new UnsupportedOperationException(paymentProcessor.name());

        // Insert payments into the database
        for (Payment payment : payments) {
            Payment.add(payment);
        }

        // Create on authorized payment listeners if needed
        if (onAuthorizedPayment != null)
            for (Payment payment : payments) {
                PayHook.onPaymentAuthorized.addAction((action, eventPayment) -> {
                    if (eventPayment.id == payment.id) {
                        action.remove(); // To make sure it only gets executed once, for this payment.
                        onAuthorizedPayment.accept(eventPayment);
                    }
                }, e -> {
                    throw new RuntimeException(e);
                }).object = now;
            }

        // Create on cancelled payment listeners if needed
        if (onCancelledPayment != null)
            for (Payment payment : payments) {
                PayHook.onPaymentCancelled.addAction((action, eventPayment) -> {
                    if (eventPayment.id == payment.id) {
                        action.remove(); // To make sure it only gets executed once, for this payment.
                        onCancelledPayment.accept(eventPayment);
                    }
                }, e -> {
                    throw new RuntimeException(e);
                }).object = now;
            }

        // Execute created payment event actions:
        // Recurring and not recurring payments are in the same payments list
        int paymentsIndex = 0;
        for (Product product : productsNOTrecurring) {
            onPaymentCreated.execute(payments.get(paymentsIndex));
            paymentsIndex++;
        }
        for (Product product : productsRecurring) {
            onPaymentCreated.execute(payments.get(paymentsIndex));
            paymentsIndex++;
        }
        return payments;
    }

    public static void throwException(Product product, Payment payment, Exception ex) throws Exception {
        throwException(product.id, payment, ex);
    }

    public static void throwException(int productId, Payment payment, Exception ex) throws Exception {
        throwException(productId, payment.id, ex);
    }

    /**
     * Throws the provided {@link Exception},
     * but also logs it in the database before throwing it.
     */
    public static void throwException(int productId, int paymentId, Exception ex) throws Exception {
        //TODO: database.insertException(productId, paymentId, ex.getMessage(), ex.getStackTrace());
        // Not sure yet if this is a good idea. Maybe let the user handle exceptions completely?
        throw ex;
    }

    /**
     * Validates the webhook event and does further type-specific stuff. <br>
     * Execute this method at your webhook endpoint. <br>
     * For example when a POST request happens on the "https://my-shop.com/paypal-hook" url. <br>
     * Note that its recommended returning a 200 status code before executing this method <br>
     * to avoid timeouts and duplicate webhook events. <br>
     *
     * @throws WebHookValidationException When the provided webhook event is not valid.
     */
    public static void receiveWebhookEvent(PaymentProcessor paymentProcessor, Map<String, String> header, String body)
            throws Exception {
        //System.err.println("Received webhook! " + new Date());
        //System.err.println("Header: " + new UtilsMap().mapToStringWithLineBreaks(header));
        //System.err.println("Body: " + body);
        long now = System.currentTimeMillis();
        if (paymentProcessor.equals(PaymentProcessor.PAYPAL)) {

            // VALIDATE PAYPAL WEBHOOK NOTIFICATION
            PaypalWebhookEvent event = new PaypalWebhookEvent(paypalWebhookId, paypalWebhookEventTypes, header, body);
            if (!paypalUtils.isWebhookEventValid(event)) {
                System.err.println("Received invalid PayPal webhook event.");
                return;
            }
            JsonObject resource = event.getBody().getAsJsonObject("resource");

            // EXECUTE ACTION FOR EVENT
            switch (event.getEventType()) {
                case "CHECKOUT.ORDER.APPROVED": { // One time payments only
                    //System.out.println("CHECKOUT.ORDER.APPROVED \n"+event.getBodyString());
                    String orderId = resource.get("id").getAsString();
                    List<Payment> payments = Payment.wherePaypalOrderId().is(orderId).get();
                    if (payments.isEmpty())
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", failed to find payment order id '" + orderId + "' in local database).");
                    long totalCharge = 0;
                    for (Payment p : payments) {
                        totalCharge += p.charge;
                    }
                    Order capturedOrder = paypalUtils.captureOrder(paypalV2, orderId).result();
                    long capturedCharge = new Converter().toSmallestCurrency(capturedOrder.purchaseUnits().get(0).payments().captures().get(0).amount());
                    if (totalCharge != capturedCharge)
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", expected paid amount of '" + totalCharge + "' but got '" + capturedCharge + "').");
                    String captureId = capturedOrder.purchaseUnits().get(0).payments().captures().get(0).id();
                    for (Payment payment : payments) {
                        if (payment.timestampAuthorized == 0) {
                            payment.timestampAuthorized = now;
                            payment.paypalCaptureId = captureId;
                            Payment.update(payment);
                            onPaymentAuthorized.execute(payment);
                        }
                    }
                    break;
                }
                case "PAYMENT.SALE.COMPLETED": { // Recurring payments only
                    //System.out.println("PAYMENT.SALE.COMPLETED \n"+event.getBodyString());
                    // custom_id is set at subscription creation and is equal to the first paymentId of the subscription
                    Payment firstPayment = Payment.whereId().is(Integer.valueOf(resource.get("custom").getAsString())).get().get(0); // custom == custom_id (idk why they call it only custom)
                    Money moneyPaid = new Money()
                            .value(resource.get("amount").getAsJsonObject().get("total").getAsString())
                            .currencyCode(resource.get("amount").getAsJsonObject().get("currency").getAsString());
                    long amountPaid = new Converter().toSmallestCurrency(moneyPaid);
                    if (firstPayment.isPending()) {
                        // First payment done on a subscription
                        if (firstPayment.charge != amountPaid)
                            throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", expected paid amount of '" + firstPayment.charge + "' but got '" + amountPaid + "').");
                        /*
                        // Not necessary, capture is done automatically it seems for subscriptions
                        JsonObject capture = paypalUtils.captureSubscription(firstPayment.paypalSubscriptionId,
                                moneyPaid);
                        if(!capture.get("status").getAsString().equals("COMPLETED"))
                            throw new Exception(PaymentProcessor.PAYPAL+": Failed to capture the initial subscription payment for: "+firstPayment.toPrintString());
                         */
                        if (firstPayment.timestampAuthorized == 0) {
                            firstPayment.timestampAuthorized = now;
                            Payment.update(firstPayment);
                            onPaymentAuthorized.execute(firstPayment);
                        }
                    } else { // Not the first payment
                        Product product = Product.get(firstPayment.productId);
                        if (product.charge != amountPaid)
                            throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", expected paid amount of '" + product.charge + "' but got '" + amountPaid + "').");
                        Payment newPayment = firstPayment.clone();
                        newPayment.id = Payment.create(firstPayment.userId, amountPaid, product.currency, product.paymentInterval)
                                .id;
                        newPayment.url = null;
                        newPayment.charge = amountPaid;
                        newPayment.timestampCreated = now;
                        newPayment.timestampAuthorized = now;
                        newPayment.timestampRefunded = 0;
                        newPayment.timestampExpires = now + 100000;
                        newPayment.timestampCancelled = 0;
                        Payment.add(newPayment);
                        onPaymentAuthorized.execute(newPayment);
                    }
                    break;
                }
                case "BILLING.SUBSCRIPTION.CANCELLED": { // Recurring payments
                    String subscriptionId = resource.get("id").getAsString();
                    List<Payment> payments = Payment.wherePaypalSubscriptionId().is(subscriptionId).get();
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", failed to find payments with subscription id '" + subscriptionId + "' in local database).");
                    new Subscription(payments).cancel(); // Cancels the last payment
                    break;
                }
                case "PAYMENT.CAPTURE.REFUNDED": {
                    //* Scenario 1:
                    //     * Paypal checkout payment with multiple procuts, thus
                    //     * there are multiple payments, but custom_id only contains the payment id
                    //     * of the first. Thus, we need to search for all payments with the same paypal ids.
                    //     * Stripe provides an order id in its refund event thus its easier to get all the payments.
                    String paymentId = resource.get("custom_id").getAsString();
                    List<Payment> payments = Payment.whereId().is(Integer.valueOf(paymentId)).get();
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", failed to find payment with id '" + paymentId + "' in local database).");
                    Payment firstPayment = payments.get(0);
                    if (firstPayment.isRecurring())
                        payments = Payment.wherePaypalSubscriptionId().is(firstPayment.paypalSubscriptionId).get();
                    else
                        payments = Payment.wherePaypalOrderId().is(firstPayment.paypalOrderId).get();
                    long amountRefunded = new Converter().toSmallestCurrency(new Money()
                            .value(resource.get("amount").getAsJsonObject().get("value").getAsString())
                            .currencyCode(resource.get("amount").getAsJsonObject().get("currency_code").getAsString()));
                    receiveRefund(amountRefunded, payments);
                }
                case "PAYMENT.SALE.REFUNDED": {
                    // SAME AS ABOVE
                    String paymentId = resource.get("custom").getAsString(); // Same as custom_id
                    List<Payment> payments = Payment.whereId().is(Integer.valueOf(paymentId)).get();
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", failed to find payment with id '" + paymentId + "' in local database).");
                    Payment firstPayment = payments.get(0);
                    if (firstPayment.isRecurring())
                        payments = Payment.wherePaypalSubscriptionId().is(firstPayment.paypalSubscriptionId).get();
                    else
                        payments = Payment.wherePaypalOrderId().is(firstPayment.paypalOrderId).get();
                    long amountRefunded = new Converter().toSmallestCurrency(new Money()
                            .value(resource.get("amount").getAsJsonObject().get("value").getAsString())
                            .currencyCode(resource.get("amount").getAsJsonObject().get("currency_code").getAsString()));
                    receiveRefund(amountRefunded, payments);
                }
                default:
                    throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", invalid event-type: " + event.getEventType() + ").");
            }

        } else if (paymentProcessor.equals(PaymentProcessor.STRIPE)) {
            UtilsStripe utilsStripe = new UtilsStripe();
            utilsStripe.handleEvent(header, body, stripeWebhookSecret);
        } else
            throw new IllegalArgumentException("Unknown payment processor: " + paymentProcessor);
        // TODO ADD NEW PAYMENT PROCESSOR
    }

    /**
     * Logic for receiving refund from a webhook event.
     *
     * @throws Exception when the amount to refund could not be covered with the provided payments.
     */
    public static void receiveRefund(long refundAmount, List<Payment> payments) throws Exception {
        long now = System.currentTimeMillis();
        for (int i = payments.size() - 1; i >= 0; i--) { // Start with the last/newest payment (more relevant for subscriptions)
            Payment payment = payments.get(i);
            if (refundAmount == 0) break;
            if (refundAmount < payment.charge) {
                payment.charge -= refundAmount;
                refundAmount = 0;
            } else {
                refundAmount -= payment.charge;
                payment.charge = 0;
            }
            payment.timestampRefunded = now;
            Payment.update(payment);
            onPaymentRefunded.execute(payment);
        }
        if (refundAmount > 0) {
            //
            // Instead of throwing an exception we create a new refund payment with negative charge.
            throw new Exception("The amount to refund could not be covered with the provided payments! Open amount to refund: " + refundAmount);
        }
    }

    /**
     * Sets {@link Payment#timestampCancelled} to now and
     * executes the {@link PayHook#onPaymentCancelled}, for the provided payment. <br>
     * If the payment is a subscription does an API-request to cancel it. <br>
     * When dealing with subscriptions it could be easier
     * to call {@link Subscription#cancel()} instead of this method. <br>
     *
     * @param payment payment to cancel.
     * @throws Exception if the provided payments list has one or more payments with different {@link PaymentProcessor}s or
     *                   {@link Payment#url}.
     * @see PayHook#onPaymentCancelled
     */
    public static synchronized void cancelPayment(Payment payment) throws Exception {
        if (payment.isCancelled()) return;
        long now = System.currentTimeMillis();

        if (payment.isRecurring()) {
            if (payment.isPayPalSupported()) {
                paypalUtils.cancelSubscription(payment.paypalSubscriptionId);
            } else if (payment.isStripeSupported()) {
                com.stripe.model.Subscription.retrieve(
                                Objects.requireNonNull(payment.stripeSubscriptionId))
                        .cancel();
            } else
                throw new IllegalArgumentException("Unknown payment processor: " + payment.getPaymentProcessor());
            // TODO ADD NEW PAYMENT PROCESSOR
        } // else is normal payment
        payment.timestampCancelled = now;
        Payment.update(payment);
        onPaymentCancelled.execute(payment);

        // Add future cancel event if there is still time left for this subscription.
        if(payment.isRecurring()){
            Subscription sub = new Subscription(payment);
            long millisLeft = sub.getMillisLeftWithPuffer();
            if(millisLeft > 0 && PendingPaymentCancel.whereId().is(payment.id).get().isEmpty()){
                PendingPaymentCancel.createAndAdd(payment.id, System.currentTimeMillis() + millisLeft);
            }
        }
    }

    /**
     * @see #refundPayments(List, Consumer)
     */
    public static void refundPayment(Payment payment) throws Exception {
        refundPayments(payment);
    }

    /**
     * @see #refundPayments(List, Consumer)
     */
    public static void refundPayments(Payment... payments) throws Exception {
        List<Payment> list = new ArrayList<>(payments.length);
        Collections.addAll(list, payments);
        refundPayments(list, null);
    }

    /**
     * Does one refund API-request for multiple payments. <br>
     * Some things to keep in mind: <br>
     * - {@link #onPaymentRefunded} is only executed once the refund confirmation (webhook event) was received by the payment processor. <br>
     * - The refund amount will be subtracted from the {@link Payment#charge}(s) once the confirmation was received. <br>
     * - This method will NOT cancel your payments. If you want to cancel them,
     * you must call {@link #cancelPayment(Payment)} too (relevant for subscriptions). <br>
     *
     * @param payments        must contain only payments that have the same {@link PaymentProcessor}, and the same {@link Payment#url}.
     * @param onRefundPayment can be null. Executed for each payment when its refund webhook event was received.
     * @throws Exception if the provided payments list has one or more payments with different {@link PaymentProcessor}s or
     *                   {@link Payment#url}.
     * @see #receiveRefund(long, List)
     */
    public static void refundPayments(List<Payment> payments, Consumer<Payment> onRefundPayment) throws Exception {
        long now = System.currentTimeMillis();
        Payment firstPayment = payments.get(0);
        PaymentProcessor processor = firstPayment.getPaymentProcessor();
        for (Payment p : payments) {
            if (p.getPaymentProcessor() != processor)
                throw new Exception("All provided payments must have the same payment processor! " + processor + "!=" + p.getPaymentProcessor());
        }
        if (firstPayment.isRecurring()) { // SUBSCRIPTIONS
            if (firstPayment.isPayPalSupported()) {
                for (Payment p : payments) {
                    if (!Objects.equals(firstPayment.paypalSubscriptionId, p.paypalSubscriptionId))
                        throw new Exception("All provided payments must have the same id! Failed at: " + p);
                    paypalUtils.refundSubscription(paypalV2, p.paypalSubscriptionId, p.timestampCreated,
                            new Converter().toPayPalMoney(p.currency, p.charge), "Refund for subscription: " + p.productName);
                }
            } else if (firstPayment.isStripeSupported()) {
                for (Payment p : payments) {
                    if (!Objects.equals(firstPayment.stripeSubscriptionId, p.stripeSubscriptionId))
                        throw new Exception("All provided payments must have the same id! Failed at: " + p);
                    Refund.create(new RefundCreateParams.Builder()
                            .setAmount(p.charge)
                            .setPaymentIntent(firstPayment.stripePaymentIntentId)
                            .build());
                }
            } else
                throw new IllegalArgumentException("Unknown payment processor: " + firstPayment.getPaymentProcessor());
            // TODO ADD NEW PAYMENT PROCESSOR

        } else { // ONE TIME PAYMENT AKA AN ORDER OF ONE OR MULTIPLE PRODUCTS
            if (firstPayment.isPayPalSupported()) {
                String reason = "Refund for product(s):";
                long totalCharge = 0;
                for (Payment p : payments) {
                    if (!Objects.equals(firstPayment.paypalOrderId, p.paypalOrderId))
                        throw new Exception("All provided payments must have the same id! Failed at: " + p);
                    totalCharge += p.charge;
                    reason += "  " + p.productQuantity + "x " + p.productName;
                }
                paypalUtils.refundPayment(paypalV2, firstPayment.paypalCaptureId,
                        new Converter().toPayPalMoney(firstPayment.currency, totalCharge), reason);

            } else if (firstPayment.isStripeSupported()) {
                long totalCharge = 0;
                for (Payment p : payments) {
                    if (!Objects.equals(firstPayment.stripePaymentIntentId, p.stripePaymentIntentId))
                        throw new Exception("All provided payments must have the same id! Failed at: " + p);
                    totalCharge += p.charge;
                }
                Refund.create(new RefundCreateParams.Builder()
                        .setAmount(totalCharge)
                        .setPaymentIntent(firstPayment.stripePaymentIntentId)
                        .build());

            } else
                throw new IllegalArgumentException("Unknown payment processor: " + firstPayment.getPaymentProcessor());
            // TODO ADD NEW PAYMENT PROCESSOR
        }

        if (onRefundPayment != null) {
            for (Payment payment : payments) {
                onPaymentRefunded.addAction((action, refundPayment) -> {
                    if (payment.id == refundPayment.id) {
                        action.remove();
                        onRefundPayment.accept(refundPayment);
                    }
                }, ex -> {
                    throw new RuntimeException(ex); // Not expected to happen
                }, false, now);
            }
        }
    }
}
