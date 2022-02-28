package com.osiris.payhook;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.PayPalWebHookEventValidator;
import com.osiris.payhook.paypal.PaypalJsonUtils;
import com.osiris.payhook.paypal.PaypalWebhookEvent;
import com.osiris.payhook.paypal.codec.binary.Base64;
import com.osiris.payhook.paypal.custom.PayPalREST;
import com.osiris.payhook.utils.Converter;
import com.paypal.api.payments.Plan;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.WebhookEndpoint;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
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
    private static final List<Consumer<PaymentEvent>> actionsOnMissedPayment = new ArrayList<>();
    private static final List<Consumer<PaymentEvent>> actionsOnReceivedPayment = new ArrayList<>();

    // Stripe specific:
    private static String stripeSecretKey;
    public static String stripeEventType = "PAYMENT.AUTHORIZATION.CREATED";

    // PayPal specific:
    private static String paypalClientId;
    private static String paypalClientSecret;
    private static String paypalBase64EncodedCredentials;
    private static PayPalREST paypalREST;
    private static APIContext paypalV1;
    private static PayPalHttpClient paypalV2;
    public static String paypalEventType = "PAYMENT.AUTHORIZATION.CREATED";

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
        if (!databaseUrl.contains("db_name")) throw new SQLException("Your databaseUrl must contain 'db_name' as database name, so it can be replaced later!");
        if(isSandbox) databaseUrl = databaseUrl.replace("db_name", "payhook_sandbox");
        else databaseUrl = databaseUrl.replace("db_name", "payhook");
        database = new PayHookDatabase(DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword), isSandbox);
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(600000); // 1h
                    checkForMissedPayments();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        isInitialised = true;
    }

    public static void initCommandLineTool(){
        if(commandLineThread!=null) commandLineThread.interrupt();
        commandLineThread = new Thread(() -> {
            try(BufferedReader in = new BufferedReader(new InputStreamReader(System.in))){
                try(PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out))){
                    out.println("Initialised PayHooks' command line tool. To exit it enter 'exit', for a list of commands enter 'help'.");
                    boolean exit = false;
                    String command = null;
                    while (!exit){
                        command = in.readLine();
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
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        commandLineThread.start();
    }

    /**
     * Sets the PayPal credentials and initialises its APIs/SDKs. <br>
     * Also creates the required Webhook if needed. <br>
     * @param clientId See PayPals' docs <a href="https://developer.paypal.com/api/rest/#link-getcredentials">here</a> for details.
     * @param clientSecret See PayPals' docs <a href="https://developer.paypal.com/api/rest/#link-getcredentials">here</a> for details.
     * @param webhookUrl Something like this: "https://my-shop.com/paypal-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String)} when receiving a webhook notification/event
     *                   on that url.
     */
    public static void initPayPal(String clientId, String clientSecret, String webhookUrl) throws IOException, HttpErrorException {
        PayHook.paypalClientId = clientId;
        PayHook.paypalClientSecret = clientSecret;

        if (isSandbox) {
            paypalREST = new PayPalREST(clientId, clientSecret, PayPalREST.Mode.SANDBOX);
            paypalV1 = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2 = new PayPalHttpClient(new PayPalEnvironment.Sandbox(clientId, clientSecret));
        } else {
            paypalREST = new PayPalREST(clientId, clientSecret, PayPalREST.Mode.LIVE);
            paypalV1 = new APIContext(clientId, clientSecret, "live");
            paypalV2 = new PayPalHttpClient(new PayPalEnvironment.Live(clientId, clientSecret));
        }
        paypalBase64EncodedCredentials = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
        boolean containsWebhookUrl = false;
        for (JsonElement e :
                paypalREST.getWebhooks()) {
            JsonObject webhook = e.getAsJsonObject();
            String url = webhook.get("url").getAsString();
            if(url.equals(webhookUrl)){
                containsWebhookUrl = true;
                break;
            }
        }
        if(!containsWebhookUrl) paypalREST.createWebhook(webhookUrl, paypalEventType);
    }

    public static void initStripe(String secretKey, String webhookUrl) throws StripeException {
        PayHook.stripeSecretKey = secretKey;
        Stripe.apiKey = secretKey;
        Map<String, Object> params = new HashMap<>();
        params.put("limit", "100");
        boolean containsWebhookUrl = false;
        for (WebhookEndpoint webhook :
                WebhookEndpoint.list(params).getData()) {
            if(webhook.getUrl().equals(webhookUrl)){
                containsWebhookUrl = true;
                break;
            }
        }
        if(!containsWebhookUrl){
            Map<String, Object> params2 = new HashMap<>();
            params2.put("url", webhookUrl);
            List<String> list = new ArrayList<>(1);
            list.add(stripeEventType);
            params2.put("enabled_events", list);
            WebhookEndpoint.create(params2);
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
     */
    public static Product putProduct(int id, long priceInSmallestCurrency,
                              String currency, String name, String description,
                              PaymentType paymentType, int customBillingIntervallInDays) throws StripeException, SQLException, PayPalRESTException, IOException, HttpErrorException {
        Converter converter = new Converter();
        // TODO also link webhook urls
        Product newProduct = new Product(id, priceInSmallestCurrency, currency, name, description, paymentType, customBillingIntervallInDays,
                null, null);
        Product dbProduct = database.getProductById(id);
        if (dbProduct == null) {
            dbProduct = new Product(id, priceInSmallestCurrency, currency, name, description, paymentType, customBillingIntervallInDays,
                    null, null);
            if (paypalClientId != null && paypalClientSecret != null) {
                paypalREST.createProduct(dbProduct);
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = converter.toPayPalPlan(dbProduct);
                    plan.create(paypalV1);
                    dbProduct.paypalPlanId = plan.getId();
                }
            }
            if (stripeSecretKey != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.create(converter.toStripeProduct(dbProduct, isStripeSandbox));
                dbProduct.stripeProductId = stripeProduct.getId();
                com.stripe.model.Price stripePrice = com.stripe.model.Price.create(converter.toStripePrice(dbProduct));
                dbProduct.stripePriceId = stripePrice.getId();
            }
            database.insertProduct(dbProduct);
        }

        newProduct.paypalProductId = dbProduct.paypalProductId;
        newProduct.stripeProductId = dbProduct.stripeProductId;

        if (compareProducts(newProduct, dbProduct)) {
            database.updateProduct(newProduct);

            if (paypalClientId != null && paypalClientSecret != null && dbProduct.paypalProductId != null) {
                // Note that PayPal doesn't store products, but only plans, in its databases.
                // Thus, products don't need to get updated, except plans
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = Plan.get(paypalV1, dbProduct.paypalProductId);
                    plan.update(paypalV1, converter.toPayPalPlanPatch(dbProduct)); // TODO
                }
            }
            if (stripeSecretKey != null && dbProduct.stripeProductId != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.retrieve(dbProduct.stripeProductId);
                stripeProduct.update(converter.toStripeProduct(dbProduct, isStripeSandbox));
                com.stripe.model.Price stripePrice = com.stripe.model.Price.retrieve(dbProduct.stripePriceId);
                stripePrice.update(converter.toStripePrice(dbProduct));
            }
        }
        return newProduct;
    }

    /**
     * Updates the {@link Product}s details in your local  and in
     * the database of its {@link PaymentProcessor}.
     */
    public static Product updateProduct(Product product){
        // TODO
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, PaymentProcessor, String, String, List)} for details. <br>
     */
    public static Payment createPayment(String userId, Product product, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return createPayments(userId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, PaymentProcessor, String, String, List)} for details. <br>
     */
    public static Payment createPayment(String userId, Product product, int quantity, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(5);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return createPayments(userId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Creates a new pending {@link Payment} which is due in 3 hours, for each {@link Product}. <br>
     * Redirect your user to {@link Payment#payUrl} to complete the payment. <br>
     * Note that {@link Product}s WITHOUT recurring payments get grouped together <br>
     * and can be paid over the same url. <br>
     * You can listen for payment completion with {@link #onPayment(int, Consumer)}. <br>
     * @param userId Unique identifier of the buying user.
     * @param products List of {@link Product}s the user wants to buy.
     *                Cannot be null, empty, or contain products with different currencies.
     *                 If the user wants the same {@link Product} twice for example, simply add it twice to this list.
     * @param paymentProcessor The users' desired {@link PaymentProcessor}.
     * @param successUrl Redirect the user to this url on a successful checkout.
     * @param cancelUrl Redirect the user to this url on an aborted checkout.
     */
    public static List<Payment> createPayments(String userId, List<Product> products, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        Converter converter = new Converter();
        Objects.requireNonNull(products);
        if (products.size() == 0) throw new Exception("Products array cannot be empty!");
        List<Product> productsNOTrecurring = new ArrayList<>();
        List<Product> productsRecurring = new ArrayList<>(1);
        for (Product p :
                products) {
            if(paymentProcessor.equals(PaymentProcessor.STRIPE) && !p.isStripeSupported())
                throw new Exception("Product with id '"+p.productId+"' does not support Stripe because of empty fields in the database!");
            else if(paymentProcessor.equals(PaymentProcessor.PAYPAL) && !p.isPayPalSupported()){
                throw new Exception("Product with id '"+p.productId+"' does not support PayPal because of empty fields in the database!");
            }
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
                int paymentId = database.paymentsId.incrementAndGet();
                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl);
                for (Product p :
                        productsAndQuantity.keySet()) {
                    int quantity = productsAndQuantity.get(p);
                    paramsBuilder.addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setName(p.name)
                                    .setDescription(p.description)
                                    .setQuantity((long) quantity)
                                    .setPrice("{{" + p.stripePriceId + "}}")
                                    .build());
                    payments.add(new Payment(paymentId, p.productId, userId, quantity,
                            (quantity * p.priceInSmallestCurrency), p.currency,
                            p.name, null, null, now,
                            null,
                            null, 0, null));
                }
                Session session = Session.create(paramsBuilder.build());
                for (Payment payment :
                        payments) {
                    payment.payUrl = session.getUrl();
                    database.insertPayment(payment);
                }
            }
            for (Product p :
                    productsRecurring) {
                int paymentId = database.paymentsId.incrementAndGet();
                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl);
                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setName(p.name)
                                .setDescription(p.description)
                                .setQuantity((long) 1)
                                .setPrice("{{" + p.stripePriceId + "}}")
                                .build());
                Session session = Session.create(paramsBuilder.build());
                Payment payment = new Payment(paymentId, p.productId, userId, 1,
                        p.priceInSmallestCurrency, p.currency,
                        p.name, session.getUrl(), null, now,
                        null,
                        null, 0,null);
                payments.add(payment);
                database.insertPayment(payment);
            }
        } else if(paymentProcessor.equals(PaymentProcessor.PAYPAL)){ // PAYPAL
            if(productsNOTrecurring.size() > 0){
                int paymentId = database.paymentsId.incrementAndGet();
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
                        productsAndQuantity.keySet()) {
                    currency = p.currency;
                    int quantity = productsAndQuantity.get(p);
                    priceTotal += p.priceInSmallestCurrency;
                    items.add(new Item().name(p.name).description(p.description)
                            .unitAmount(new Money().currencyCode(p.currency).value(converter.toPayPalCurrency(p).getValue())).quantity(""+quantity)
                            .category("DIGITAL_GOODS"));
                    payments.add(new Payment(paymentId, p.productId, userId, quantity,
                            (quantity * p.priceInSmallestCurrency), p.currency,
                            p.name, null, null, now,
                            null,
                            null,0, null));
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

                HttpResponse<Order> response = paypalV2.execute(request);
                if (response.statusCode() != 201) throw new PayPalRESTException("Failed to create order! PayPal returned error code '"+response.statusCode()+"'.");
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
                    payment.payUrl = payUrl;
                    database.insertPayment(payment);
                }
            }
            for (Product p :
                    productsRecurring) {
                int paymentId = database.paymentsId.incrementAndGet();
                String[] arr = paypalREST.createSubscription(brandName, p.paypalPlanId, successUrl, cancelUrl);
                Payment payment = new Payment(paymentId, p.productId, userId, 1,
                        p.priceInSmallestCurrency, p.currency,
                        p.name, arr[1], arr[0], now,
                        null,
                        null,0, null);
                payments.add(payment);
                database.insertPayment(payment);
            }
        }
        return payments;
    }

    /**
     * Does not compare payment specific details.
     * @return true if the provided {@link Product}s have different essential information.
     */
    private static boolean compareProducts(Product p1, Product p2) {
        if (p1.productId != p2.productId)
            return true;
        if (p1.priceInSmallestCurrency != p2.priceInSmallestCurrency)
            return true;
        if (!p1.currency.equals(p2.currency))
            return true;
        if (!p1.name.equals(p2.name))
            return true;
        if (!p1.description.equals(p2.description))
            return true;
        if (p1.paymentType != p2.paymentType)
            return true;
        return p1.customBillingIntervallInDays != p2.customBillingIntervallInDays;
    }

    /**
     * Executed when a valid payment was received on a WebHook. <br>
     */
    public static void onPayment(int paymentId, Consumer<PaymentEvent> action) {
        synchronized (actionsOnReceivedPayment) {
            Consumer<PaymentEvent> actualAction = paymentEvent -> {
                if(paymentEvent.payment.paymentId == paymentId){
                    action.accept(paymentEvent);
                }
            };
            actionsOnReceivedPayment.add(actualAction);
        }
    }

    /**
     * Execute this method at your webhook endpoint. <br>
     * For example on the "https://my-shop.com/paypal-hook" url. <br>
     * Note that its recommended returning a 200 status code before executing this method <br>
     * to avoid timeouts and duplicate webhook events. <br>
     */
    public static void receiveWebhookEvent(PaymentProcessor paymentProcessor, Map<String, String> header, String body){

    }

    /**
     * The provided WebHook event gets validated. <br>
     * If its valid and really a 'payment received' event, then the
     * code at {@link Order#onPaymentReceived(Consumer)} gest executed. <br>
     * Note that the event type must be: PAYMENT.AUTHORIZATION.CREATED <br>
     */
    public void runPayPalPaymentReceived(PaypalWebhookEvent paypalWebhookEvent) throws WebHookValidationException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, SignatureException, ParseBodyException, InvalidKeyException, HttpErrorException {
        PayPalWebHookEventValidator validator = new PayPalWebHookEventValidator(paypalClientId, paypalClientSecret);
        validator.validateWebhookEvent(paypalWebhookEvent);
        final String eventTypeAuthorization = "PAYMENT.AUTHORIZATION.CREATED";
        if(paypalWebhookEvent.getEventType().equalsIgnoreCase(eventTypeAuthorization)){
            String captureURL = null;
            JsonArray arrayLinks = paypalWebhookEvent.getBody().getAsJsonObject("resource").getAsJsonArray("links");
            for (JsonElement e:
                    arrayLinks) {
                JsonObject obj = e.getAsJsonObject();
                if (obj.get("rel").getAsString().equals("capture")){
                    captureURL = obj.get("href").getAsString();
                    break;
                }
            }
            if (captureURL==null) throw new WebHookValidationException("Failed to find 'capture' url inside of: "+new GsonBuilder().setPrettyPrinting().create().toJson(arrayLinks));
            new PaypalJsonUtils().postJsonAndGetResponse(captureURL,"{}", paypalBase64EncodedCredentials, 201);
            // TODO
            Payment payment = new Payment();
            database.insertPayment(payment);

        } else
            throw new WebHookValidationException("Event type '"+paypalWebhookEvent.getEventType()+"' is wrong and should be '"+eventTypeAuthorization+"'!");
    }

    /**
     * The provided WebHook event gets validated. <br>
     * If its valid and really a 'payment received' event, then the
     * code at {@link Order#onPaymentReceived(Consumer)} gest executed.
     */
    public void runStripePaymentReceived() {

    }

    /**
     * Executed when the due payment hasn't been received yet. <br>
     * Note that the user is given one extra day to pay. <br>
     * Also note that this is executed every hour, until the order is cancelled. <br>
     */
    public static void onMissedPayment(Consumer<PaymentEvent> action) {
        synchronized (actionsOnMissedPayment) {
            actionsOnMissedPayment.add(action);
        }
    }

    private static void executeMissedPayment(PaymentEvent paymentEvent) {
        synchronized (actionsOnMissedPayment) {
            for (Consumer<PaymentEvent> action : actionsOnMissedPayment) {
                action.accept(paymentEvent);
            }
        }
    }

    /**
     * Checks for payments where the {@link Payment#timestampReceived} now is in the past
     * and the {@link Payment#isPending} is still true.
     */
    public static void checkForMissedPayments() throws SQLException {
        List<Order> orders = database.getOrders();
        long now = System.currentTimeMillis();
        long extraTime = 86400000L; // Give the user one extra day to pay
        long month = 2629800000L + extraTime;
        long month3 = 7889400000L + extraTime;
        long month6 = 15778800000L + extraTime;
        long month12 = 31557600000L + extraTime;
        for (Order o :
                orders) {
            if (o.isRecurring()) {
                if (o.isBillingInterval1Month()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month)
                        executeMissedPayment(new PaymentEvent(o));

                } else if (o.isBillingInterval3Months()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month3)
                        executeMissedPayment(new PaymentEvent(o));
                } else if (o.isBillingInterval6Months()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month6)
                        executeMissedPayment(new PaymentEvent(o));
                } else if (o.isBillingInterval12Months()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month12)
                        executeMissedPayment(new PaymentEvent(o));
                } else { // Custom payment intervall
                    long custom = o.getCustomBillingIntervallInDays() * 86400000L; // xdays multiplied with 1 day as millisecond
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > custom)
                        executeMissedPayment(new PaymentEvent(o));
                }
            }
        }
    }
}
