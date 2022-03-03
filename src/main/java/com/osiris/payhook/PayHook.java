package com.osiris.payhook;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.ParseHeaderException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.utils.Converter;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.WebhookEndpoint;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.*;
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
    private static final Map<Integer, Consumer<PaymentEvent>> paymentIdsAndActionsOnReceivedPayment = new HashMap<>();

    // Stripe specific:
    public static List<String> stripeWebhookEventTypes = Arrays.asList("PAYMENT.AUTHORIZATION.CREATED"); // TODO

    // PayPal specific:
    public static BraintreeGateway braintreeGateway;
    public static List<String> braintreeWebhookEventTypes = Arrays.asList(); // TODO

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
     * Sets the Braintree credentials and initialises its APIs/SDKs. <br>
     * <p style="color:red;"> Important: </p>You must create the webhook with the allowed types yourself. See Braintrees' webhook docs
     * <a href="https://developer.paypal.com/braintree/docs/guides/webhooks/overview">here</a> for details. <br>
     * @param merchantId See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param publicKey See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param privateKey See Braintrees' docs <a href="https://developer.paypal.com/braintree/articles/control-panel/important-gateway-credentials">here</a> for details.
     * @param webhookUrl Something like this: "https://my-shop.com/braintree-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String)} when receiving a webhook notification/event
     *                   on that url.
     */
    public static void initBraintree(String merchantId, String publicKey, String privateKey, String webhookUrl) throws IOException, HttpErrorException {
        if (isSandbox) {
            PayHook.braintreeGateway = new BraintreeGateway(
                    Environment.SANDBOX,
                    merchantId,
                    publicKey,
                    privateKey
            );
        } else {
            PayHook.braintreeGateway = new BraintreeGateway(
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
     * Also creates the required webhook if needed. <br>
     * @param secretKey See Stripes' docs <a href="https://stripe.com/docs/keys">here</a> for details.
     * @param webhookUrl Something like this: "https://my-shop.com/stripe-hook". <p style="color:red;"> Important: </p>Remember that you must
     *                   run {@link #receiveWebhookEvent(PaymentProcessor, Map, String)} when receiving a webhook notification/event
     *                   on that url.
     */
    public static void initStripe(String secretKey, String webhookUrl) throws StripeException {
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
            params2.put("enabled_events", stripeWebhookEventTypes);
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
                              PaymentType paymentType, int customBillingIntervallInDays) throws StripeException, SQLException, IOException, HttpErrorException {
        Converter converter = new Converter();

        Product newProduct = new Product(id, priceInSmallestCurrency, currency, name, description, paymentType, customBillingIntervallInDays,
                null, null);
        Product dbProduct = database.getProductById(id);
        if (dbProduct == null) {
            dbProduct = new Product(id, priceInSmallestCurrency, currency, name, description, paymentType, customBillingIntervallInDays,
                    null, null);
            if (braintreeGateway != null) {
                /*paypalREST.createProduct(dbProduct); // TODO
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = converter.toPayPalPlan(dbProduct);
                    plan.create(paypalV1);
                    dbProduct.paypalPlanId = plan.getId();
                }*/
            }
            if (Stripe.apiKey != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.create(converter.toStripeProduct(dbProduct, isSandbox));
                dbProduct.stripeProductId = stripeProduct.getId();
                com.stripe.model.Price stripePrice = com.stripe.model.Price.create(converter.toStripePrice(dbProduct));
                dbProduct.stripePriceId = stripePrice.getId();
            }
            database.putProduct(dbProduct);
        }

        newProduct.braintreeProductId = dbProduct.braintreeProductId;
        newProduct.stripeProductId = dbProduct.stripeProductId;

        if (compareProducts(newProduct, dbProduct)) {
            database.updateProduct(newProduct);

            if (braintreeGateway!=null) {
                /*if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = Plan.get(paypalV1, dbProduct.paypalProductId);
                    plan.update(paypalV1, converter.toPayPalPlanPatch(dbProduct)); // TODO
                }*/
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
     * Updates the {@link Product}s details in your local and in
     * the database of its {@link PaymentProcessor}.
     */
    public static Product updateProduct(Product product){
        // TODO
        return product;
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, List, PaymentProcessor, String, String)}} for details. <br>
     */
    public static Payment createPayment(String userId, Product product, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return createPayments(userId, products, paymentProcessor, successUrl, cancelUrl)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, List, PaymentProcessor, String, String)} for details. <br>
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
     * You can listen for payment completion with {@link #onReceivedPayment(int, Consumer)}. <br>
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
                throw new Exception("Product with id '"+p.id +"' does not support Stripe because of empty fields in the database!");
            else if(paymentProcessor.equals(PaymentProcessor.BRAINTREE) && !p.isBraintreeSupported()){
                throw new Exception("Product with id '"+p.id +"' does not support Braintree because of empty fields in the database!");
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
                    payments.add(new Payment(paymentId, p.id, userId, quantity,
                            (quantity * p.priceInSmallestCurrency), p.currency,
                            p.name, null, null, now,
                            null,
                            null, 0, null));
                }
                Session session = Session.create(paramsBuilder.build());
                for (Payment payment :
                        payments) {
                    payment.payUrl = session.getUrl();
                    payment.stripePaymentIntentId = session.getPaymentIntentObject().getId();
                    database.insertPayment(payment);
                }
            }
            for (Product p :
                    productsRecurring) {
                int paymentId = database.paymentsId.incrementAndGet();
                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
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
                Payment payment = new Payment(paymentId, p.id, userId, 1,
                        p.priceInSmallestCurrency, p.currency,
                        p.name, session.getUrl(), null, now,
                        null,
                        null, 0,null);
                payment.stripeSubscriptionId = session.getSubscriptionObject().getId();
                payments.add(payment);
                database.insertPayment(payment);
            }
        } else if(paymentProcessor.equals(PaymentProcessor.BRAINTREE)){ // BRAINTREE
            /* TODO
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
                    payment.payUrl = payUrl;
                    payment.braintreeOrderId = response.result().id();
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
            }*/
        }
        return payments;
    }

    /**
     * Does not compare payment specific details.
     * @return true if the provided {@link Product}s have different essential information.
     */
    private static boolean compareProducts(Product p1, Product p2) {
        if (p1.id != p2.id)
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
        return p1.customPaymentIntervall != p2.customPaymentIntervall;
    }

    /**
     * Executed when a valid payment was received on via a webhook event. <br>
     */
    public static void onReceivedPayment(int paymentId, Consumer<PaymentEvent> action) {
        synchronized (paymentIdsAndActionsOnReceivedPayment) {
            Consumer<PaymentEvent> actualAction = null;
            actualAction = paymentEvent -> {
                if(paymentEvent.payment.paymentId == paymentId){
                    action.accept(paymentEvent);
                }
            };
            paymentIdsAndActionsOnReceivedPayment.put(paymentId, actualAction);
        }
    }

    /**
     * Validates the webhook event and does further type-specific stuff. <br>
     * Execute this method at your webhook endpoint. <br>
     * For example when a POST request happens on the "https://my-shop.com/paypal-hook" url. <br>
     * Note that its recommended returning a 200 status code before executing this method <br>
     * to avoid timeouts and duplicate webhook events. <br>
     * Supported webhook types for PayPal: <br>
     * - PAYMENT.AUTHORIZATION.CREATED | {@link #onReceivedPayment(int, Consumer)}<br>
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
            //TODO
        } else{
            //TODO
        }
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
     * Executed when a payment (also recurring payment, aka subscription) was cancelled.
     */
    public static void onCancelledSubscription(){
        // TODO BILLING.SUBSCRIPTION.CANCELLED
    }
}
