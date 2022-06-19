package com.osiris.payhook;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.InvalidChangeException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.MyPayPal;
import com.osiris.payhook.paypal.PayPalWebHookEventValidator;
import com.osiris.payhook.paypal.PaypalWebhookEvent;
import com.osiris.payhook.paypal.codec.binary.Base64;
import com.osiris.payhook.stripe.UtilsStripe;
import com.osiris.payhook.utils.Converter;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.Order;
import com.paypal.orders.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.*;

/**
 * Still work in progress. <br>
 * Release planned in v3.0 <br>
 */
public final class PayHook {
    /**
     * Actions for this event are executed, when a payment is created
     * via {@link PayHook#createPayments(String, List, PaymentProcessor, String, String)}.
     */
    public static final com.osiris.events.Event<PaymentEvent> paymentCreatedEvent = new com.osiris.events.Event<>();
    /**
     * Actions for this event are executed,
     * when a valid payment (authorized payment) was received via a webhook notification. <br>
     * This happens for example when the user goes to {@link Payment#url} and completes the steps, <br>
     * or when an automatic payment happens on an already running subscription.
     */
    public static final com.osiris.events.Event<PaymentEvent> paymentAuthorizedEvent = new com.osiris.events.Event<>();
    /**
     * Actions for this event are executed,
     * when a payment was created, but never actually paid (authorized)
     * within the time period the {@link Payment#url} was valid, or when
     * a webhook notification was received that states a missed payment on an already running subscription,
     * or that the subscription was cancelled/refunded, or that a product was refunded. <br>
     */
    public static final com.osiris.events.Event<PaymentEvent> paymentCancelledEvent = new com.osiris.events.Event<>();
    /**
     * How long is a stripe payment url valid?
     * It is valid as long as the stripe session is valid.
     * The default is 24h hours.
     *
     * @see <a href="https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-expires_at">Stripe docs</a>
     */
    public static final long stripeUrlTimeoutMs = 86400000;
    public static String brandName;
    public static boolean isInitialised = false;
    public static boolean isSandbox = false;


    // Stripe specific:
    public static Stripe stripe;
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread stripeThread;
    private static boolean isStripeWebhookActive = false;
    /**
     * payment_intent.succeeded: Occurs whenever payment is authorized/successful. <br>
     * invoice.created: Payment on a subscription created, but not finalized. <br>
     * invoice.paid: Payment on a subscription. <br>
     * customer.subscription.deleted: Occurs whenever a customer’s subscription ends. <br>
     *
     * @see <a href="https://stripe.com/docs/api/webhook_endpoints/update">Stripe docs</a>
     */
    public static List<String> stripeWebhookEventTypes = Arrays.asList("payment_intent.succeeded", "invoice.created",
            "invoice.paid", "customer.subscription.deleted");


    // Braintree specific:
    public static BraintreeGateway braintree;
    public static List<String> braintreeWebhookEventTypes = Arrays.asList(); // TODO
    /**
     * Additional time in milliseconds that is given to the user (default 1 day),
     * to pay a recurring payment, to prevent direct expiration of the subscription.
     * Example: Subscription runs for 30 days and would expire directly at the end of that day
     * resulting in a {@link #paymentCancelledEvent}, if the payment processor takes a little longer
     * to send the payment for the next 30 days. Thus, 1 additional day (default) is given to receive the payment
     * for the next 30 days.
     */
    //TODO NOT NEEDED: REMOVE WHEN DONE WITH DATABASE: public static long recurringPaymentAddedTimeMs;
    private static Thread commandLineThread;
    private static Thread expiredPaymentsCheckerThread;
    /**
     * A secret key specific to the webhook that is used to validate it.
     */
    private static String stripeWebhookSecret;
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread braintreeThread;
    private static final boolean isBraintreeWebhookActive = false;


    // PayPal specific:
    public static MyPayPal myPayPal;
    public static APIContext paypalV1;
    public static PayPalHttpClient paypalV2;
    private static String paypalClientId;
    private static String paypalClientSecret;
    private static String paypalBase64EncodedCredentials;
    private static String paypalWebhookId;
    private static final List<String> paypalWebhookEventTypes = Arrays.asList("BILLING.SUBSCRIPTION.CANCELLED", "PAYMENT.SALE.COMPLETED",
            "CHECKOUT.ORDER.APPROVED");
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread paypalThread;
    private static boolean isPayPalWebhookActive = false;
    /**
     * How long is a PayPal payment url valid? 3 hours. <br>
     * From the docs: Once redirected, the API caller has 3 hours for the payer to approve the order and either authorize or capture the order.
     *
     * @see <a href="https://developer.paypal.com/docs/api/orders/v2/#orders-create-response">PayPal docs</a>
     */
    public static long paypalUrlTimeoutMs = 10800000;

    /**
     * If {@link #isSandbox} = true then the "payhook_sandbox" database will get created/used, otherwise
     * the default "payhook" database.
     * Remember to set your {@link PaymentProcessor} credentials, before
     * creating/updating any {@link Product}s. <br>
     *
     * @param databaseUrl      Example: "jdbc:mysql://localhost:3306/db_name?serverTimezone=Europe/Rome".
     * @param databaseUsername Example: "root".
     * @param databasePassword Example: "".
     * @throws SQLException     When the databaseUrl does not contain "db_name" or another error happens during database initialisation.
     * @throws RuntimeException when there is an error in the thread, which checks for missed payments in a regular interval.
     */
    public static synchronized void init(String brandName, String databaseUrl, String databaseUsername, String databasePassword, boolean isSandbox) throws SQLException {
        if (isInitialised) return;
        PayHook.brandName = brandName;
        PayHook.isSandbox = isSandbox;
        if(!isSandbox  && (databaseUrl.contains("sandbox") || databaseUrl.contains("test")))
            throw new SQLException("You are NOT running in sandbox mode, thus your database-url/name CANNOT contain 'sandbox' or 'test'!");
        if (isSandbox && (!databaseUrl.contains("sandbox") && !databaseUrl.contains("test")))
            throw new SQLException("You are running in sandbox mode, thus your database-url/name must contain 'sandbox' or 'test'!");
        Database.url = databaseUrl;
        Database.username = databaseUsername;
        Database.password = databasePassword;

        expiredPaymentsCheckerThread = new Thread(() -> {
            try {
                while (true) {
                    long now = System.currentTimeMillis();
                    for (Payment pendingPayment : Payment.getPendingPayments()) {
                        if (now > pendingPayment.timestampExpires) {
                            pendingPayment.timestampCancelled = now;
                            Payment.update(pendingPayment);
                            paymentCancelledEvent.execute(new PaymentEvent(Product.get(pendingPayment.productId), pendingPayment));
                        }
                    }
                    Thread.sleep(3600000); // 1h
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        expiredPaymentsCheckerThread.start();

        // Since we received an authorized payment for a subscription
        // we create the future/next payment already to be able to
        // catch it in the future.
        paymentAuthorizedEvent.addAction((action, event) -> {
            Payment currentPayment = event.payment;
            if (currentPayment.isRecurring() && !currentPayment.isRefund()) {
                long futureTime = System.currentTimeMillis() + Payment.Intervall.toMilliseconds(currentPayment.intervall);
                Payment futurePayment = currentPayment.clone();
                futurePayment.id = Payment.create(currentPayment.userId, currentPayment.charge, currentPayment.currency, currentPayment.intervall)
                        .id;
                futurePayment.timestampCreated = futureTime;
                futurePayment.timestampExpires = futureTime + currentPayment.getUrlTimeoutMs();
                futurePayment.timestampAuthorized = 0;
                futurePayment.timestampCancelled = 0;
                Payment.add(futurePayment);
            }
        }, ex -> {
            throw new RuntimeException(ex);
        });

        isInitialised = true;
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
            }
            stripeWebhookSecret = targetWebhook.getSecret();
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
    public static void initPayPal(String clientId, String clientSecret, String webhookUrl) throws IOException, HttpErrorException {
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(clientSecret);
        Objects.requireNonNull(webhookUrl);
        PayHook.paypalClientId = clientId;
        PayHook.paypalClientSecret = clientSecret;

        if (isSandbox) {
            myPayPal = new MyPayPal(clientId, clientSecret, MyPayPal.Mode.SANDBOX);
            paypalV1 = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2 = new PayPalHttpClient(new PayPalEnvironment.Sandbox(clientId, clientSecret));
        } else {
            myPayPal = new MyPayPal(clientId, clientSecret, MyPayPal.Mode.LIVE);
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
                    myPayPal.getWebhooks()) {
                JsonObject webhook = e.getAsJsonObject();
                String url = webhook.get("url").getAsString();
                if (url.equals(webhookUrl)) {
                    paypalWebhookId = webhook.get("id").getAsString();
                    containsWebhookUrl = true;
                    break;
                }
            }
            if (!containsWebhookUrl) myPayPal.createWebhook(webhookUrl, paypalWebhookEventTypes);
        }
    }

    /**
     * Call this method after setting the credentials for your payment processors. <br>
     * If the provided id doesn't exist in the database, the product gets created/inserted. <br>
     * If the provided id exists in the database and the new provided values differ from the values in the database, it gets updated. <br>
     * The above also happens for the {@link Product}s saved on the databases of the payment processors. <br>
     *
     * @param id                           The unique identifier of this product.
     * @param charge      E.g., 100 cents to charge $1.00 or 100 to charge ¥100, a zero-decimal currency.
     *                                     The amount value supports up to eight digits (e.g., a value of 99999999 for a USD charge of $999,999.99).
     * @param currency                     Three-letter <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO currency code</a>,
     *                                     in lowercase. Must be a <a href="https://stripe.com/docs/currencies">supported currency</a>.
     * @param name                         The name of the product.
     * @param description                  The products' description.
     * @param paymentIntervall The payment intervall in days. Only relevant for recurring payments. See {@link Payment.Intervall} for useful defaults.
     * @throws InvalidChangeException if you tried to change the payment type of the product.
     */
    public static Product putProduct(int id, long charge,
                                     String currency, String name, String description,
                                     int paymentIntervall) throws Exception {
        Converter converter = new Converter();
        Product p = null;
        try{p = Product.get(id);} catch (Exception ignored) {}
        if (p == null) { // Product not existing yet
            p = new Product(id, charge, currency, name, description, paymentIntervall);

            if (myPayPal != null) { // Create paypal produt
                JsonObject response = myPayPal.createProduct(p);
                p.paypalProductId = response.get("id").getAsString();
                if (p.isRecurring()) {
                    com.paypal.api.payments.Plan plan = converter.toPayPalPlan(p);
                    plan.create(paypalV1);
                    p.paypalPlanId = plan.getId();
                }
            }

            if (braintree != null) ; // TODO

            if (Stripe.apiKey != null) { // Create stripe product
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.create(converter.toStripeProduct(p, isSandbox));
                p.stripeProductId = stripeProduct.getId();
                com.stripe.model.Price stripePrice = com.stripe.model.Price.create(converter.toStripePrice(p));
                p.stripePriceId = stripePrice.getId();
            }

            Product.add(p);
        }

        Product newProduct = new Product(id, charge, currency, name, description, paymentIntervall);
        if (newProduct.id != p.id || newProduct.charge != p.charge || !newProduct.currency.equals(p.currency) ||
        !newProduct.name.equals(p.name) || !newProduct.description.equals(p.description) || newProduct.paymentIntervall != p.paymentIntervall) {
            p.id = id;
            p.charge = charge; // TODO find way of updating payments
            p.currency = currency; // TODO find way of updating payments
            p.name = name;
            p.description = description;
            p.paymentIntervall = paymentIntervall; // TODO find way of updating payments

            if (myPayPal != null) {
                if (p.isRecurring()) {
                    com.paypal.api.payments.Plan plan = com.paypal.api.payments.Plan.get(paypalV1, p.paypalProductId);
                    plan.update(paypalV1, converter.toPayPalPlanPatch(p)); // TODO
                }
            }
            if (Stripe.apiKey != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.retrieve(p.stripeProductId);
                stripeProduct.update(converter.toStripeProduct(p, isSandbox));
                com.stripe.model.Price stripePrice = com.stripe.model.Price.retrieve(p.stripePriceId);
                stripePrice.update(converter.toStripePrice(p));
            }

            Product.update(p);
        }
        return p;
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * @see #createPayments(String, List, PaymentProcessor, String, String)
     */
    public static Payment createPayment(String userId, Product product, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return createPayments(userId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}, with custom quantity. <br>
     * @see #createPayments(String, List, PaymentProcessor, String, String)
     */
    public static Payment createPayment(String userId, Product product, int quantity, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return createPayments(userId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Creates a new pending {@link Payment} for each provided {@link Product}, which expires in a {@link PaymentProcessor} specific time
     * (see {@link #paymentCancelledEvent}).
     * Redirect your user to {@link Payment#url} to complete the payment.
     * Note that {@link Product}s WITHOUT recurring payments get grouped together
     * and can be paid over the same url.
     * You can listen for payment authorization/completion at {@link #paymentAuthorizedEvent}.
     *
     * @param userId           {@link Payment#userId}
     * @param products         List of {@link Product}s the user wants to buy.
     *                         Cannot be null, empty, or contain products with different currencies.
     *                         If the user wants the same {@link Product} twice for example, simply add it twice to this list.
     * @param paymentProcessor The users' desired {@link PaymentProcessor}.
     * @param successUrl       Redirect the user to this url on a successful checkout.
     * @param cancelUrl        Redirect the user to this url on an aborted checkout.
     */
    public static List<Payment> createPayments(String userId, List<Product> products, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
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
            else if (paymentProcessor.equals(PaymentProcessor.BRAINTREE) && !p.isBraintreeSupported())
                throw new Exception("Product with id '" + p.id + "' does not support " + PaymentProcessor.BRAINTREE + " because of empty fields in the database!");

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
                                    .setName(p.name)
                                    .setDescription(p.description)
                                    .setQuantity((long) quantity)
                                    .setPrice("{{" + p.stripePriceId + "}}")
                                    .build());
                    payments.add(Payment.create(userId,
                            (quantity * p.charge), p.currency,
                            p.paymentIntervall,
                            null, p.id, p.name, quantity,
                            now, now + stripeUrlTimeoutMs, 0, 0,
                            null, null, null,
                            null, null, null));
                }
                Session session = Session.create(paramsBuilder.build());
                for (Payment payment :
                        payments) {
                    payment.url = session.getUrl();
                    payment.stripePaymentIntentId = session.getPaymentIntentObject().getId();
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
                                .setName(product.name)
                                .setDescription(product.description)
                                .setQuantity((long) 1)
                                .setPrice("{{" + product.stripePriceId + "}}")
                                .build());
                Session session = Session.create(paramsBuilder.build());
                Payment payment = Payment.create(userId,
                        product.charge, product.currency,
                        product.paymentIntervall,
                        session.getUrl(),
                        product.id, product.name, 1, now, now + stripeUrlTimeoutMs, 0, 0,
                        null, session.getSubscriptionObject().getId(), null,
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
                for (Product p :
                        productsNOTrecurring) {
                    currency = p.currency;
                    int quantity = productsAndQuantity.get(p);
                    priceTotal += p.charge;
                    items.add(new Item().name(p.name).description(p.description)
                            .unitAmount(new Money().currencyCode(p.currency).value(converter.toPayPalCurrency(p).getValue())).quantity("" + quantity)
                            .category("DIGITAL_GOODS"));
                    payments.add(Payment.create(userId,
                            (quantity * p.charge), p.currency,
                            p.paymentIntervall, null,
                            p.id, p.name, quantity, now, now + paypalUrlTimeoutMs, 0, 0,
                            null, null, null,
                            null, null, null));
                }
                PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                        .description(brandName).softDescriptor(brandName)
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
                String[] arr = myPayPal.createSubscription(brandName, p.paypalPlanId, successUrl, cancelUrl);
                Payment payment = Payment.create(userId,
                        p.charge, p.currency, p.paymentIntervall, arr[1],
                        p.id, p.name, 1, now, now + paypalUrlTimeoutMs, 0, 0,
                        null, null, null,
                        null, arr[0], null);
                payments.add(payment);
            }
        } else
            throw new UnsupportedOperationException(paymentProcessor.name());

        // Insert payments into the database
        for (Payment payment : payments) {
            Payment.add(payment);
        }

        // Execute created payment event actions:
        // Recurring and not recurring payments are in the same payments list
        int paymentsIndex = 0;
        for (Product product : productsNOTrecurring) {
            paymentCreatedEvent.execute(new PaymentEvent(product, payments.get(paymentsIndex)));
            paymentsIndex++;
        }
        for (Product product : productsRecurring) {
            paymentCreatedEvent.execute(new PaymentEvent(product, payments.get(paymentsIndex)));
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
     * @throws WebHookValidationException When the provided webhook event is not valid.
     */
    public static void receiveWebhookEvent(PaymentProcessor paymentProcessor, Map<String, String> header, String body)
            throws Exception {
        long now = System.currentTimeMillis();
        if (paymentProcessor.equals(PaymentProcessor.PAYPAL)) {

            // VALIDATE PAYPAL WEBHOOK NOTIFICATION
            PayPalWebHookEventValidator validator = new PayPalWebHookEventValidator(paypalClientId, paypalClientSecret);
            PaypalWebhookEvent event = new PaypalWebhookEvent(paypalWebhookId, paypalWebhookEventTypes,
                    validator.parseAndGetHeader(header), validator.parseAndGetBody(body));
            validator.validateWebhookEvent(event);
            JsonObject resource = event.getBody().getAsJsonObject("resource");

            // EXECUTE ACTION FOR EVENT
            switch (event.getEventType()) {
                case "CHECKOUT.ORDER.APPROVED": { // One time payments
                    String orderId = resource.get("id").getAsString();
                    List<Payment> payments = Payment.get("paypalOrderId = "+orderId);
                    if (payments.isEmpty())
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find payment order id '" + orderId + "' in local database).");
                    long totalCharge = 0;
                    for (Payment p : payments) {
                        totalCharge += p.charge;
                    }
                    Order capturedOrder = myPayPal.captureOrder(paypalV2, orderId).result();
                    long capturedCharge = new Converter().toSmallestCurrency(capturedOrder.purchaseUnits().get(0).payments().captures().get(0).amount());
                    if (totalCharge != capturedCharge)
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", expected paid amount of '" + totalCharge + "' but got '" + capturedCharge + "').");
                    String captureId = capturedOrder.purchaseUnits().get(0).payments().captures().get(0).id();
                    for (Payment payment : payments) {
                        if (payment.timestampAuthorized == 0) {
                            payment.timestampAuthorized = now;
                            payment.paypalCaptureId = captureId;
                            Payment.update(payment);
                            paymentAuthorizedEvent.execute(new PaymentEvent(Product.get(payment.productId), payment));
                        }
                    }
                    break;
                }
                case "PAYMENT.SALE.COMPLETED": { // Recurring payments
                    String paymentId = resource.get("id").getAsString();
                    com.paypal.payments.Capture capture = myPayPal.capturePayment(paypalV2, paymentId).result();
                    String subscriptionId = myPayPal.findSubscriptionId(paymentId);
                    List<Payment> payments = Payment.getPendingPayments("paypalSubscriptionId = "+subscriptionId);
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", failed to find pending payments with subscription id '" + subscriptionId + "' in local database).");
                    if (payments.size() > 1) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", there are multiple (" + payments.size() + ") pending payments with the subscription id '" + subscriptionId + "' in local database).");
                    Payment payment = payments.get(0);
                    long capturedCharge = new Converter().toSmallestCurrency(capture.amount());
                    if (payment.charge != capturedCharge)
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.PAYPAL + ", expected paid amount of '" + payment.charge + "' but got '" + capturedCharge + "').");
                    if (payment.timestampAuthorized == 0) {
                        payment.timestampAuthorized = now;
                        Payment.update(payment);
                        paymentAuthorizedEvent.execute(new PaymentEvent(Product.get(payment.productId), payment));
                    }
                    break;
                }
                case "BILLING.SUBSCRIPTION.CANCELLED": { // Recurring payments
                    String subscriptionId = resource.get("id").getAsString();
                    List<Payment> payments = Payment.get("paypalSubscriptionId = "+subscriptionId);
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find payments with subscription id '" + subscriptionId + "' in local database).");
                    cancelPayments(payments);
                    break;
                }
                default:
                    throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", invalid event-type: " + event.getEventType() + ").");
            }

        } else if (paymentProcessor.equals(PaymentProcessor.STRIPE)) {
            UtilsStripe utilsStripe = new UtilsStripe();
            Event event = utilsStripe.checkWebhookEvent(body, header, stripeWebhookSecret);
            if (event == null)
                throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", validation failed).");
            // Deserialize the nested object inside the event
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;
            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                // Deserialization failed, probably due to an API version mismatch.
                // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
                // instructions on how to handle this case, or return an error here.
                throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", deserialization failed).");
            }
            // Handle the event

            switch (event.getType()) {
                case "payment_intent.succeeded": { // One time payments
                    PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                    List<Payment> payments = Payment.get("stripePaymentIntentId = "+paymentIntent.getId());
                    if (payments.isEmpty())
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find payment intent id '" + paymentIntent.getId() + "' in local database).");
                    long totalCharge = 0;
                    for (Payment p : payments) {
                        totalCharge += p.charge;
                    }
                    if (totalCharge != paymentIntent.getAmount())
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", expected paid amount of '" + totalCharge + "' but got '" + paymentIntent.getAmount() + "').");
                    paymentIntent.capture();
                    for (Payment payment : payments) {
                        if (payment.timestampAuthorized == 0) {
                            payment.timestampAuthorized = now;
                            payment.stripeChargeId = paymentIntent.getInvoiceObject().getCharge();
                            Payment.update(payment);
                            paymentAuthorizedEvent.execute(new PaymentEvent(Product.get(payment.productId), payment));
                        }
                    }
                    break;
                }
                case "invoice.created": { // Recurring payments
                    break; // Return 2xx status code to auto-finalize the invoice and receive a invoice.paid event next
                }
                case "invoice.paid": { // Recurring payments
                    Invoice invoice = (Invoice) stripeObject;
                    Subscription subscription = invoice.getSubscriptionObject();
                    List<Payment> payments = Payment.getPendingPayments("stripeSubscriptionId = "+subscription.getId());
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find pending payments with stripe_subscription_id '" + subscription.getId() + "' in local database).");
                    if (payments.size() > 1) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", there are multiple (" + payments.size() + ") pending payments with the stripe_subscription_id '" + subscription.getId() + "' in local database).");
                    Payment payment = payments.get(0);
                    if (payment.charge != invoice.getChargeObject().getAmount())
                        throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", expected paid amount of '" + payment.charge + "' but got '" + invoice.getChargeObject().getAmount() + "').");
                    if (payment.timestampAuthorized == 0) {
                        payment.timestampAuthorized = now;
                        payment.stripeChargeId = invoice.getCharge();
                        Payment.update(payment);
                        paymentAuthorizedEvent.execute(new PaymentEvent(Product.get(payment.productId), payment));
                    }
                    break;
                }
                case "customer.subscription.deleted": { // Recurring payments
                    Subscription subscription = (Subscription) stripeObject; // TODO check if this actually works
                    List<Payment> payments = Payment.get("stripeSubscriptionId = "+subscription.getId());
                    if (payments.isEmpty()) throw new WebHookValidationException(
                            "Received invalid webhook event (" + PaymentProcessor.STRIPE + ", failed to find payments with stripe_subscription_id '" + subscription.getId() + "' in local database).");
                    cancelPayments(payments);
                    break;
                }
                default:
                    throw new WebHookValidationException("Received invalid webhook event (" + PaymentProcessor.STRIPE + ", invalid event-type: " + event.getType() + ").");
            }
        } else
            throw new IllegalArgumentException("Unknown payment processor: "+paymentProcessor);
        // TODO ADD NEW PAYMENT PROCESSOR
    }

    /**
     * @see #cancelPayments(List)
     */
    public static void cancelPayment(Payment payment) throws Exception {
        refundPayments(payment);
    }

    /**
     * @see #cancelPayments(List)
     */
    public static void cancelPayments(Payment... payments) throws Exception {
        List<Payment> list = new ArrayList<>(payments.length);
        Collections.addAll(list, payments);
        refundPayments(list);
    }

    /**
     * Sets {@link Payment#timestampCancelled} to now and
     * executes the {@link PayHook#paymentCancelledEvent}, for all the provided payments. <br>
     * If the payment is a subscription does an API-request to cancel it also at the {@link PaymentProcessor}.
     * @param payments must contain only payments that have the same {@link PaymentProcessor}, and the same {@link Payment#url}.
     * @throws Exception if the provided payments list has one or more payments with different {@link PaymentProcessor}s or
     * {@link Payment#url}.
     * @see PayHook#paymentCancelledEvent
     */
    public static void cancelPayments(List<Payment> payments) throws Exception {
        long now = System.currentTimeMillis();
        Payment firstPayment = payments.get(0);
        PaymentProcessor processor = firstPayment.getPaymentProcessor();
        for (Payment p : payments) {
            if (p.getPaymentProcessor() != processor)
                throw new Exception("All provided payments must have the same payment processor! " + processor + "!=" + p.getPaymentProcessor());
        }
        for (Payment payment : payments) { // Execute payment cancel events
            if (!payment.isCancelled()){
                if (payment.isRecurring()) {
                    if (payment.isPayPalSupported())
                    {
                        if (!Objects.equals(firstPayment.paypalSubscriptionId, payment.paypalSubscriptionId))
                            throw new Exception("All provided payments must have the same id! Failed at: " + payment);
                        myPayPal.cancelSubscription(firstPayment.paypalSubscriptionId);
                    }
                    else if (payment.isStripeSupported())
                    {
                        if (!Objects.equals(firstPayment.stripeSubscriptionId, payment.stripeSubscriptionId))
                            throw new Exception("All provided payments must have the same id! Failed at: " + payment);
                        Subscription.retrieve(
                                Objects.requireNonNull(firstPayment.stripeSubscriptionId))
                                .cancel();
                    }
                    else throw new IllegalArgumentException("Unknown payment processor: " + payment.getPaymentProcessor());
                    // TODO ADD NEW PAYMENT PROCESSOR
                }
                payment.timestampCancelled = now;
                paymentCancelledEvent.execute(new PaymentEvent(Product.get(payment.productId), payment));
            }
        }
    }

    /**
     * @see #refundPayments(List)
     */
    public static void refundPayment(Payment payment) throws Exception {
        refundPayments(payment);
    }

    /**
     * @see #refundPayments(List)
     */
    public static void refundPayments(Payment... payments) throws Exception {
        List<Payment> list = new ArrayList<>(payments.length);
        Collections.addAll(list, payments);
        refundPayments(list);
    }

    /**
     * Does one refund API-request for multiple payments. <br>
     * If you want to have one refund API-request per payment, use {@link #refundPayment(Payment)} on each
     * payment manually instead of this method. <br>
     * This method creates new refund payments (and directly authorizes them, which causes
     * {@link #paymentCreatedEvent} and {@link #paymentAuthorizedEvent} to be executed)
     * for each of the provided payments, with the {@link Payment#charge}
     * negated.
     * Note that this method will NOT cancel your payments. If you want to cancel them,
     * you must call {@link #cancelPayments(List)} too.
     * @param payments must contain only payments that have the same {@link PaymentProcessor}, and the same {@link Payment#url}.
     * @throws Exception if the provided payments list has one or more payments with different {@link PaymentProcessor}s or
     * {@link Payment#url}.
     */
    public static void refundPayments(List<Payment> payments) throws Exception {
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
                    myPayPal.refundSubscription(paypalV2, p.paypalSubscriptionId, p.timestampCreated,
                            new Converter().toPayPalMoney(p.currency, p.charge), "Refund for subscription: "+p.productName);
                }
            } else if (firstPayment.isStripeSupported()) {
                for (Payment p : payments) {
                    if (!Objects.equals(firstPayment.stripeSubscriptionId, p.stripeSubscriptionId))
                        throw new Exception("All provided payments must have the same id! Failed at: " + p);
                    Refund.create(new RefundCreateParams.Builder()
                            .setAmount(p.charge)
                            .setCharge(p.stripeChargeId)
                            .setPaymentIntent(firstPayment.stripePaymentIntentId)
                            .build());
                }
            } else throw new IllegalArgumentException("Unknown payment processor: " + firstPayment.getPaymentProcessor());
            // TODO ADD NEW PAYMENT PROCESSOR

        } else { // ONE TIME PAYMENT AKA AN ORDER OF ONE OR MULTIPLE PRODUCTS
            if (firstPayment.isPayPalSupported()) {
                String reason = "Refund for product(s):";
                long totalCharge = 0;
                for (Payment p : payments) {
                    if (!Objects.equals(firstPayment.paypalOrderId, p.paypalOrderId))
                        throw new Exception("All provided payments must have the same id! Failed at: " + p);
                    totalCharge += p.charge;
                    reason += "  "+p.productQuantity+"x "+p.productName;
                }
                myPayPal.refundPayment(paypalV2, firstPayment.paypalCaptureId,
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

            } else throw new IllegalArgumentException("Unknown payment processor: " + firstPayment.getPaymentProcessor());
            // TODO ADD NEW PAYMENT PROCESSOR

            for (Payment payment : payments) { // Create refund payments
                Payment refundPayment = payment.clone();
                refundPayment.id = Payment.create(payment.userId, payment.charge, payment.currency, payment.intervall)
                        .id;
                refundPayment.charge = Long.parseLong("-"+payment.charge);
                refundPayment.timestampCreated = now;
                refundPayment.timestampExpires = now + 3600000; // 1h
                refundPayment.timestampAuthorized = now;
                refundPayment.timestampCancelled = 0;
                Payment.add(refundPayment);
                Product product = Product.get(payment.productId);
                paymentCreatedEvent.execute(new PaymentEvent(product, payment));
                paymentAuthorizedEvent.execute(new PaymentEvent(product, payment));
            }
        }
    }
}
