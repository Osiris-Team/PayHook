package com.osiris.payhook;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.InvalidChangeException;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.ParseHeaderException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.MyPayPal;
import com.osiris.payhook.paypal.codec.binary.Base64;
import com.osiris.payhook.stripe.UtilsStripe;
import com.osiris.payhook.utils.Converter;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;

/**
 * Still work in progress. <br>
 * Release planned in v3.0 <br>
 */
public final class PayHook {
    public static String brandName;
    public static PayHookDatabase database;
    public static boolean isInitialised = false;
    public static boolean isSandbox = false;
    private static Thread commandLineThread;
    /**
     * Actions for this event are executed, when a payment is created
     * via {@link PayHook#createPayments(String, String, List, PaymentProcessor, String, String)}.
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

    // Stripe specific:
    public static Stripe stripe;
    /**
     * How long is a stripe payment url valid?
     * It is valid as long as the stripe session is valid.
     * The default is 24h hours.
     * @see <a href="https://stripe.com/docs/api/checkout/sessions/create#create_checkout_session-expires_at">Stripe docs</a>
     */
    public static final long stripeUrlTimeoutMs = 86400000;
    /**
     * charge.succeeded: Occurs whenever a charge is successful. <br>
     * payment_intent.succeeded: Occurs when a PaymentIntent has successfully completed payment. <br>
     * customer.subscription.created: Occurs whenever a customer is signed up for a new plan. <br>
     * customer.subscription.deleted: Occurs whenever a customer’s subscription ends. <br>
     * @see <a href="https://stripe.com/docs/api/webhook_endpoints/update">Stripe docs</a>
     */
    public static List<String> stripeWebhookEventTypes = Arrays.asList("charge.succeeded","payment_intent.succeeded",
            "customer.subscription.created", "customer.subscription.deleted");
    /**
     * A secret key specific to the webhook that is used to validate it.
     */
    private static String stripeWebhookSecret;
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread stripeThread;
    private static boolean isStripeWebhookActive = false;

    // Braintree specific:
    public static BraintreeGateway braintree;
    public static List<String> braintreeWebhookEventTypes = Arrays.asList(); // TODO
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread braintreeThread;
    private static boolean isBraintreeWebhookActive = false;

    // PayPal specific:

    /**
     * How long is a PayPal payment url valid? 3 hours. <br>
     * From the docs: Once redirected, the API caller has 3 hours for the payer to approve the order and either authorize or capture the order.
     * @see <a href="https://developer.paypal.com/docs/api/orders/v2/#orders-create-response">PayPal docs</a>
     */
    public static long paypalUrlTimeoutMs = 10800000;
    private static String paypalClientId;
    private static String paypalClientSecret;
    private static String paypalBase64EncodedCredentials;
    private static MyPayPal myPayPal;
    private static APIContext paypalV1;
    private static PayPalHttpClient paypalV2;
    private static String paypalWebhookId;
    private static List<String> paypalWebhookEventTypes = Arrays.asList("CHECKOUT.ORDER.APPROVED", "PAYMENT.AUTHORIZATION.CREATED");
    /**
     * Handles receiving payments when no webhook.
     */
    private static Thread paypalThread;
    private static boolean isPayPalWebhookActive = false;

    /**
     * If {@link #isSandbox} = true then the "payhook_sandbox" database will get created/used, otherwise
     * the default "payhook" database.
     * Remember to set your {@link PaymentProcessor} credentials, before
     * creating/updating any {@link Product}s. <br>
     *
     * @param databaseUrl Example: "jdbc:mysql://localhost:3306/db_name?serverTimezone=Europe/Rome". Note that
     *                    PayHook will replace "db_name" with either "payhook" or "payhook_sandbox".
     * @param databaseUsername Example: "root".
     * @param databasePassword Example: "".
     * @throws SQLException When the databaseUrl does not contain "db_name" or another error happens during database initialisation.
     * @throws RuntimeException when there is an error in the thread, which checks for missed payments in a regular interval.
     */
    public static void init(String brandName, String databaseUrl, String databaseUsername, String databasePassword, boolean isSandbox) throws SQLException {
        if(isInitialised) return;
        PayHook.brandName = brandName;
        PayHook.isSandbox = isSandbox;
        String dbName = "payhook";
        if (!databaseUrl.contains("db_name")) throw new SQLException("Your databaseUrl must contain 'db_name' as database name, so it can be replaced later!");
        if(isSandbox) {
            dbName = "payhook_sanbox";
            databaseUrl = databaseUrl.replace("db_name", dbName);
        }
        else databaseUrl = databaseUrl.replace("db_name", dbName);
        database = new PayHookDatabase(dbName, DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword));
        isInitialised = true;
    }

    public static void initCommandLineTool(){
        if(commandLineThread!=null) commandLineThread.interrupt();
        commandLineThread = new Thread(() -> {
            PrintStream out = System.out;
            Scanner scanner = new Scanner(System.in);
            out.println("Initialised PayHooks' command line tool. To exit it enter 'exit', for a list of commands enter 'help'.");
            boolean exit = false;
            String command = null;
            while (!exit){
                command = scanner.nextLine();
                try{
                    if (command.equals("exit")){
                        exit = true;
                    } else if(command.equals("help") || command.equals("h")){
                        out.println("Available commands:");
                        out.println("products delete <id> | Removes the product with the given id from the local database.");
                        // TODO add commands like:
                        // payments <days> // Prints all received payments from the last <days> (if not provided 30 days is the default)
                    } else if(command.startsWith("products delete")){
                        int id = Integer.parseInt(command.replaceFirst("products delete ", "").trim());
                        database.deleteProductById(id); //TODO delete everywhere
                    } else{
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
     * @param merchantId See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param publicKey See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param privateKey See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param webhookUrl Something like this: "https://my-shop.com/braintree-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String, String)} when receiving a webhook notification/event
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
     * @param secretKey See Stripes' docs <a href="https://stripe.com/docs/keys">here</a> for details.
     * @param webhookUrl TODO If null, webhooks will not be used, instead regular REST-API requests will be made.
     *                   Something like this: "https://my-shop.com/stripe-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String, String)} when receiving a webhook notification/event
     *                   on that url.
     */
    public static void initStripe(String secretKey, String webhookUrl) throws StripeException {
        Objects.requireNonNull(secretKey);
        Objects.requireNonNull(webhookUrl);
        Stripe.apiKey = secretKey;
        if(webhookUrl == null){
            //TODO
        } else{
            isStripeWebhookActive = true;
            Map<String, Object> params = new HashMap<>();
            params.put("limit", "100");
            WebhookEndpoint targetWebhook = null;
            for (WebhookEndpoint webhook :
                    WebhookEndpoint.list(params).getData()) {
                if(webhook.getUrl().equals(webhookUrl)){
                    targetWebhook = webhook;
                    break;
                }
            }
            if(targetWebhook == null){
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
     * @param clientId See PayPals' docs <a href="https://developer.paypal.com/api/rest/#link-getcredentials">here</a> for details.
     * @param clientSecret See PayPals' docs <a href="https://developer.paypal.com/api/rest/#link-getcredentials">here</a> for details.
     * @param webhookUrl TODO If null, webhooks will not be used, instead regular REST-API requests will be made.
     *                  Something like this: "https://my-shop.com/paypal-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String, String)} when receiving a webhook notification/event
     *                   on that url.
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
        if(webhookUrl == null){
            if(paypalThread !=null) paypalThread.interrupt();
            // TODO start thread, that checks subscription payments every 12 hours
            paypalThread = new Thread(() -> {

            });
            paypalThread.start();
        } else{
            isPayPalWebhookActive = true;
            boolean containsWebhookUrl = false;
            for (JsonElement e :
                    myPayPal.getWebhooks()) {
                JsonObject webhook = e.getAsJsonObject();
                String url = webhook.get("url").getAsString();
                if(url.equals(webhookUrl)){
                    paypalWebhookId = webhook.get("id").getAsString();
                    containsWebhookUrl = true;
                    break;
                }
            }
            if(!containsWebhookUrl) myPayPal.createWebhook(webhookUrl, paypalWebhookEventTypes);
        }
    }

    /**
     * Call this method after setting the credentials for your payment processors. <br>
     * If the provided id doesn't exist in the {@link #database}, the product gets created/inserted. <br>
     * If the provided id exists in the {@link #database} and the new provided values differ from the values in the {@link #database}, it gets updated. <br>
     * The above also happens for the {@link Product}s saved on the databases of the payment processors. <br>
     *
     * @param id                           The unique identifier of this product.
     * @param priceInSmallestCurrency      E.g., 100 cents to charge $1.00 or 100 to charge ¥100, a zero-decimal currency.
     *                                     The amount value supports up to eight digits (e.g., a value of 99999999 for a USD charge of $999,999.99).
     * @param currency                     Three-letter <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO currency code</a>,
     *                                     in lowercase. Must be a <a href="https://stripe.com/docs/currencies">supported currency</a>.
     * @param name                         The name of the product.
     * @param description                  The products' description.
     * @param customBillingIntervallInDays The custom billing intervall in days. Note that billingType must be set to 5 for this to have affect.
     * @throws InvalidChangeException if you tried to change the payment type of the product.
     */
    public static Product putProduct(int id, long priceInSmallestCurrency,
                              String currency, String name, String description,
                              Payment.Type paymentType, int customBillingIntervallInDays) throws StripeException, SQLException, IOException, HttpErrorException, PayPalRESTException, InvalidChangeException {
        Converter converter = new Converter();

        Product newProduct = new Product(id, priceInSmallestCurrency, currency, name, description, paymentType, customBillingIntervallInDays,
                null, null);
        Product dbProduct = database.getProductById(id);
        if (dbProduct == null) {
            dbProduct = new Product(id, priceInSmallestCurrency, currency, name, description, paymentType, customBillingIntervallInDays,
                    null, null);
            if (myPayPal != null) {
                JsonObject response = myPayPal.createProduct(dbProduct);
                dbProduct.paypalProductId = response.get("id").getAsString();
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = converter.toPayPalPlan(dbProduct);
                    plan.create(paypalV1);
                    dbProduct.paypalPlanId = plan.getId();
                }
            }

            if(braintree != null); // TODO

            if (Stripe.apiKey != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.create(converter.toStripeProduct(dbProduct, isSandbox));
                dbProduct.stripeProductId = stripeProduct.getId();
                com.stripe.model.Price stripePrice = com.stripe.model.Price.create(converter.toStripePrice(dbProduct));
                dbProduct.stripePriceId = stripePrice.getId();
            }
            database.putProduct(dbProduct);
        }

        newProduct.paypalProductId = dbProduct.paypalProductId;
        newProduct.stripeProductId = dbProduct.stripeProductId;

        if (isNotEqual(newProduct, dbProduct)) {
            if(newProduct.paymentType != dbProduct.paymentType)
                throw new InvalidChangeException("The payment type of a product cannot be changed!");
            database.putProduct(newProduct);

            if (myPayPal !=null) {
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = com.paypal.api.payments.Plan.get(paypalV1, dbProduct.paypalProductId);
                    plan.update(paypalV1, converter.toPayPalPlanPatch(dbProduct)); // TODO
                }
            }
            if (Stripe.apiKey != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.retrieve(dbProduct.stripeProductId);
                stripeProduct.update(converter.toStripeProduct(dbProduct, isSandbox));
                com.stripe.model.Price stripePrice = com.stripe.model.Price.retrieve(dbProduct.stripePriceId);
                stripePrice.update(converter.toStripePrice(dbProduct));
            }
        }
        return newProduct;
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, String, List, PaymentProcessor, String, String)} for details. <br>
     */
    public static Payment createPayment(String buyerId, String sellerId, Product product, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return createPayments(buyerId, sellerId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}, with custom quantity. <br>
     * See {@link #createPayments(String, String, List, PaymentProcessor, String, String)} for details. <br>
     */
    public static Payment createPayment(String buyerId,  String sellerId, Product product, int quantity, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return createPayments(buyerId, sellerId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Creates a new pending {@link Payment} which expires in 3 hours, for each {@link Product}. <br>
     * Redirect your user to {@link Payment#url} to complete the payment. <br>
     * Note that {@link Product}s WITHOUT recurring payments get grouped together <br>
     * and can be paid over the same url. <br>
     * You can listen for payment completion with {@link #onAuthorizedPayment(int, Consumer)}. <br>
     * @param buyerId Unique identifier for the person buying this product(s), aka the person that sends the money.
     * @param sellerId Unique identifier for the person selling this product(s), aka the person that receives the money.
     * @param products List of {@link Product}s the user wants to buy.
     *                Cannot be null, empty, or contain products with different currencies.
     *                 If the user wants the same {@link Product} twice for example, simply add it twice to this list.
     * @param paymentProcessor The users' desired {@link PaymentProcessor}.
     * @param successUrl Redirect the user to this url on a successful checkout.
     * @param cancelUrl Redirect the user to this url on an aborted checkout.
     */
    public static List<Payment> createPayments(String buyerId, String sellerId, List<Product> products, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        Converter converter = new Converter();
        Objects.requireNonNull(products);
        if (products.size() == 0) throw new Exception("Products array cannot be empty!");

        List<Product> productsNOTrecurring = new ArrayList<>();
        List<Product> productsRecurring = new ArrayList<>();
        for (Product p :
                products) {
            if(paymentProcessor.equals(PaymentProcessor.STRIPE) && !p.isStripeSupported())
                throw new Exception("Product with id '"+p.id +"' does not support "+PaymentProcessor.STRIPE+" because of empty fields in the database!");
            else if(paymentProcessor.equals(PaymentProcessor.PAYPAL) && !p.isPayPalSupported())
                throw new Exception("Product with id '"+p.id +"' does not support "+PaymentProcessor.PAYPAL+" because of empty fields in the database!");
            else if(paymentProcessor.equals(PaymentProcessor.BRAINTREE) && !p.isBraintreeSupported())
                throw new Exception("Product with id '"+p.id +"' does not support "+PaymentProcessor.BRAINTREE+" because of empty fields in the database!");

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
                if(p.equals(p2)){
                    productsAndQuantity.merge(p, 1, Integer::sum);
                }
            }
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Payment> payments = new ArrayList<>();
        if(paymentProcessor.equals(PaymentProcessor.STRIPE)){ // STRIPE
            if(productsNOTrecurring.size() > 0){

                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl);
                for (Product p :
                        productsNOTrecurring) {
                    int paymentId = database.paymentsId.incrementAndGet();
                    int quantity = productsAndQuantity.get(p);
                    paramsBuilder.addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setName(p.name)
                                    .setDescription(p.description)
                                    .setQuantity((long) quantity)
                                    .setPrice("{{" + p.stripePriceId + "}}")
                                    .build());
                    payments.add(new Payment(paymentId, buyerId,
                            sellerId, (quantity * p.charge), p.currency,
                            null, now, null,
                            p.paymentType,
                            p.id, p.name, quantity,
                            null, null));
                }
                Session session = Session.create(paramsBuilder.build());
                for (Payment payment :
                        payments) {
                    payment.url = session.getUrl();
                    payment.stripePaymentIntentId = session.getPaymentIntentObject().getId();
                    payment.timestampExpires = new Timestamp(System.currentTimeMillis() + stripeUrlTimeoutMs);
                }
            }
            for (Product product :
                    productsRecurring) {
                int paymentId = database.paymentsId.incrementAndGet();
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
                Payment payment = new Payment(paymentId, buyerId,
                        sellerId, product.charge, product.currency,
                        session.getUrl(), now, null,
                        product.paymentType,
                        product.id, product.name, 1,
                        null, session.getSubscriptionObject().getId());

                // If true, means that this is not the first payment for this subscription
                // thus the expiry date must be set according to the billing intervall
                if(database.getPaymentBy("stripe_subscription_id", payment.stripeSubscriptionId) != null)
                    payment.timestampExpires = new Timestamp(System.currentTimeMillis() + payment.type.toMilliseconds());
                else
                    payment.timestampExpires = new Timestamp(System.currentTimeMillis() + stripeUrlTimeoutMs);
                payments.add(payment);
            }
        }
        else if(paymentProcessor.equals(PaymentProcessor.PAYPAL)){ // PAYPAL

            if(productsNOTrecurring.size() > 0){
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
                    int paymentId = database.paymentsId.incrementAndGet();
                    currency = p.currency;
                    int quantity = productsAndQuantity.get(p);
                    priceTotal += p.charge;
                    items.add(new Item().name(p.name).description(p.description)
                            .unitAmount(new Money().currencyCode(p.currency).value(converter.toPayPalCurrency(p).getValue())).quantity(""+quantity)
                            .category("DIGITAL_GOODS"));
                    payments.add(new Payment(paymentId, buyerId,
                            sellerId, (quantity * p.charge), p.currency,
                            null, now, null,
                            p.id, p.name, quantity,
                            null, null));
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
                if (response.statusCode() != 201) throw new PayPalRESTException("Failed to create order! Braintree returned error code '"+response.statusCode()+"'.");
                String payUrl = null;
                for (LinkDescription link : response.result().links()) {
                    if (link.rel().equals("approve")){
                        payUrl = link.href();
                        break;
                    }
                }
                if(payUrl == null) throw new PayPalRESTException("Failed to determine payUrl!");
                for (Payment payment :
                        payments) {
                    payment.url = payUrl;
                    payment.paypalOrderId = response.result().id();
                    payment.timestampExpires = new Timestamp(System.currentTimeMillis() + paypalUrlTimeoutMs);
                }
            }
            for (Product p :
                    productsRecurring) {
                int paymentId = database.paymentsId.incrementAndGet();
                String[] arr = myPayPal.createSubscription(brandName, p.paypalPlanId, successUrl, cancelUrl);
                Payment payment = new Payment(paymentId, buyerId,
                        sellerId, p.charge, p.currency,
                        arr[1], now, null,
                        p.id, p.name, 1,
                        null, arr[0]);

                // If true, means that this is not the first payment for this subscription
                // thus the expiry date must be set according to the billing intervall
                if(database.getPaymentBy("paypal_subscription_id", payment.paypalSubscriptionId) != null)
                    payment.timestampExpires = new Timestamp(System.currentTimeMillis() + payment.type.toMilliseconds());
                else
                    payment.timestampExpires = new Timestamp(System.currentTimeMillis() + stripeUrlTimeoutMs);
                payments.add(payment);
            }
        } else
            throw new UnsupportedOperationException(paymentProcessor.name());

        // Execute created payment event actions:
        int paymentsIndex = 0;
        for (Product product : productsNOTrecurring) {
            paymentCreatedEvent.execute(new PaymentEvent(product, payments.get(paymentsIndex)));
            paymentsIndex++;
        }
        for (Product product : productsRecurring) {
            paymentCreatedEvent.execute(new PaymentEvent(product, payments.get(paymentsIndex)));
            paymentsIndex++;
        }

        // Insert payments into the database
        for (Payment payment : payments) {
            database.insertPayment(payment);
        }
        return payments;
    }

    /**
     * Does not compare payment specific details.
     * @return true if the provided {@link Product}s have different essential information.
     */
    private static boolean isNotEqual(Product p1, Product p2) {
        if (p1.id != p2.id)
            return true;
        if (p1.charge != p2.charge)
            return true;
        if (!p1.currency.equals(p2.currency))
            return true;
        if (!p1.name.equals(p2.name))
            return true;
        if (!p1.description.equals(p2.description))
            return true;
        if (p1.paymentType != p2.paymentType)
            return true;
        return p1.customPaymentIntervall != p2.customPaymentIntervall;
    }

    /**
     * Validates the webhook event and does further type-specific stuff. <br>
     * Execute this method at your webhook endpoint. <br>
     * For example when a POST request happens on the "https://my-shop.com/paypal-hook" url. <br>
     * Note that its recommended returning a 200 status code before executing this method <br>
     * to avoid timeouts and duplicate webhook events. <br>
     * Supported webhook types for PayPal: <br>
     * - PAYMENT.AUTHORIZATION.CREATED | {@link #onAuthorizedPayment(int, Consumer)}<br>
     * - BILLING.SUBSCRIPTION.CANCELLED | {@link #onCancelledSubscription()} <br>
     * @throws WebHookValidationException When the provided webhook event is not valid.
     */
    public static void receiveWebhookEvent(PaymentProcessor paymentProcessor, Map<String, String> header, String body) throws ParseHeaderException, IOException, WebHookValidationException, ParseBodyException {
        if(paymentProcessor.equals(PaymentProcessor.BRAINTREE)){
            /* TODO
            PayPalWebHookEventValidator validator = new PayPalWebHookEventValidator(braintreeMerchantId, braintreePrivateKey);
            PaypalWebhookEvent event = new PaypalWebhookEvent(paypalWebhookId, null,
                    validator.parseAndGetHeader(header), validator.parseAndGetBody(body));
            if(!paypalREST.isWebhookEventValid(event.getHeader(), body, paypalWebhookId))
                throw new WebHookValidationException("The provided webhook event is not valid!");
            if(!UtilsLists.containsIgnoreCase(braintreeWebhookEventTypes, event.getEventType()))
                throw new WebHookValidationException("The provided webhook event type '"+event.getEventType()+"' does not match '"+ Arrays.toString(braintreeWebhookEventTypes.toArray()) +"' and thus is not valid!");

            String captureURL = null;
            JsonArray arrayLinks = event.getBody().getAsJsonObject("resource").getAsJsonArray("links");
            for (JsonElement e:
                    arrayLinks) {
                JsonObject obj = e.getAsJsonObject();
                if (obj.get("rel").getAsString().equals("capture")){
                    captureURL = obj.get("href").getAsString();
                    break;
                }
            }
            if (captureURL==null) throw new WebHookValidationException("Failed to find 'capture' url inside of: "+new GsonBuilder().setPrettyPrinting().create().toJson(arrayLinks));
            paypalREST.captureSubscription();
            new PaypalJsonUtils().postJsonAndGetResponse(captureURL,"{}", paypalBase64EncodedCredentials, 201);
            // TODO
            Payment payment = new Payment();
            database.insertPayment(payment);*/
        } else if(paymentProcessor.equals(PaymentProcessor.STRIPE)){
            UtilsStripe utilsStripe = new UtilsStripe();
            Event event = utilsStripe.checkWebhookEvent(body, header, stripeWebhookSecret);
            if(event == null) throw new WebHookValidationException("Received invalid webhook event ("+PaymentProcessor.STRIPE+", validation failed).");
            // Deserialize the nested object inside the event
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;
            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                // Deserialization failed, probably due to an API version mismatch.
                // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
                // instructions on how to handle this case, or return an error here.
                throw new WebHookValidationException("Received invalid webhook event ("+PaymentProcessor.STRIPE+", deserialization failed).");
            }
            // Handle the event
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                    Payment payment = database.getPaymentBy("stripe_payment_intent_id", paymentIntent.getId());
                    if(payment == null) throw new WebHookValidationException("Received invalid webhook event ("+PaymentProcessor.STRIPE+", failed to find payment intent id '"+paymentIntent.getId()+"' in local database).");
                    if(payment.charge != paymentIntent.getAmount())
                        throw new WebHookValidationException("Received invalid webhook event ("+PaymentProcessor.STRIPE+", expected paid amount of '"+payment.charge+"' but got '"+paymentIntent.getAmount()+"').");
                    payment.timestampAuthorized = new Timestamp(System.currentTimeMillis());
                    break;
                case "payment_method.attached":
                    PaymentMethod paymentMethod = (PaymentMethod) stripeObject;
                    System.out.println("PaymentMethod was attached to a Customer!");
                    break;
                // ... handle other event types
                default:
                    throw new WebHookValidationException("Received invalid webhook event ("+PaymentProcessor.STRIPE+", invalid event-type: "+event.getType()+").");
            }
        } else{
            //TODO
        }
    }


    private static void executeMissedPayment(PaymentEvent paymentEvent) {
        synchronized (paymentCancelledEvent) {
            for (Consumer<PaymentEvent> action : paymentCancelledEvent) {
                action.accept(paymentEvent);
            }
        }
    }

    /**
     * Executed when a payment (also recurring payment, aka subscription) was cancelled.
     */
    public static void onCancelledSubscription(){
        // TODO BILLING.SUBSCRIPTION.CANCELLED
    }
}
