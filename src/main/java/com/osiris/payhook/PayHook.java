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
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
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
public class PayHook {
    public final PayHookDatabase database;
    private Thread commandLineThread;
    private final List<Consumer<PaymentEvent>> actionsOnMissedPayment = new ArrayList<>();
    private final List<Consumer<PaymentEvent>> actionsOnReceivedPayment = new ArrayList<>();

    // Stripe specific:
    public boolean isStripeSandbox;
    private String stripeSecretKey;

    // PayPal specific:
    public boolean isPaypalSandbox;
    private String paypalClientId;
    private String paypalClientSecret;
    private String paypalBase64EncodedCredentials;
    private PayPalREST paypalREST;
    private APIContext paypalV1ApiContext;
    private PayPalEnvironment paypalV2ApiContext;

    /**
     * PayHook makes payments easy. Example workflow: <br>
     * 1. Set the credentials of your payment processors. <br>
     * 2. Create/Update {@link Product}s. <br>
     * 3. User selects a {@link PaymentProcessor} and creates an {@link Order} that contains selected {@link Product}s. <br>
     * 4. Redirect user to the payment processor, to complete the payment. <br>
     * 5. Get notified once the payment was received. <br>
     * <br>
     * Example with Stripe: <br>
     * 1. {@link #createStripeOrder(long, String, String, String)} <br>
     * 2. {@link Order#createPaymentUrl} <br>
     * 3. <br>
     *
     * @param databaseUrl
     * @param databaseUsername
     * @param databasePassword
     * @throws SQLException
     * @throws RuntimeException when there is an error in the thread, which checks for missed payments in a regular interval.
     */
    public PayHook(String databaseName, String databaseUrl, String databaseUsername, String databasePassword) throws SQLException {
        database = new PayHookDatabase(databaseName, DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword));
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
    }

    public void initCommandLineTool(){
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

    public void initPayPal(boolean isSandbox, String clientId, String clientSecret) {
        this.paypalClientId = clientId;
        this.paypalClientSecret = clientSecret;
        this.isPaypalSandbox = isSandbox;

        if (isSandbox) {
            paypalREST = new PayPalREST(clientId, clientSecret, PayPalREST.Mode.SANDBOX);
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2ApiContext = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        } else {
            paypalREST = new PayPalREST(clientId, clientSecret, PayPalREST.Mode.LIVE);
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "live");
            paypalV2ApiContext = new PayPalEnvironment.Live(clientId, clientSecret);
        }
        paypalBase64EncodedCredentials = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
    }

    public void initStripe(boolean isSandbox, String secretKey) {
        this.stripeSecretKey = secretKey;
        this.isStripeSandbox = isSandbox;
        Stripe.apiKey = secretKey;
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
     * @param billingType                  Value between 0 and 5: <br>
     *                                     0 = one time payment <br>
     *                                     1 = recurring payment every month <br>
     *                                     2 = recurring payment every 3 months <br>
     *                                     3 = recurring payment every 6 months <br>
     *                                     4 = recurring payment every 12 months <br>
     *                                     5 = recurring payment with a custom intervall <br>
     * @param customBillingIntervallInDays The custom billing intervall in days. Note that billingType must be set to 5 for this to have affect.
     */
    public Product putProduct(int id, long priceInSmallestCurrency,
                              String currency, String name, String description,
                              int billingType, int customBillingIntervallInDays) throws StripeException, SQLException, PayPalRESTException, IOException, HttpErrorException {
        Converter converter = new Converter();
        // TODO also link webhook urls
        Product newProduct = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                null, null);
        Product dbProduct = database.getProductById(id);
        if (dbProduct == null) {
            dbProduct = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                    null, null);
            if (paypalClientId != null && paypalClientSecret != null) {
                paypalREST.createProduct(dbProduct);
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = converter.toPayPalPlan(dbProduct);
                    plan.create(paypalV1ApiContext);
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
                    com.paypal.api.payments.Plan plan = Plan.get(paypalV1ApiContext, dbProduct.paypalProductId);
                    plan.update(paypalV1ApiContext, converter.toPayPalPlanPatch(dbProduct)); // TODO
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
    public Product updateProduct(Product product){
        // TODO
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, PaymentProcessor, String, String, List)} for details. <br>
     */
    public Payment createPayment(String userId, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl, Product product) throws Exception {
        List<Product> products = new ArrayList<>(1);
        products.add(product);
        return createPayments(userId, paymentProcessor, successUrl, cancelUrl, products)
                .get(0);
    }

    /**
     * Convenience method for creating a single {@link Payment} for a single {@link Product}. <br>
     * See {@link #createPayments(String, PaymentProcessor, String, String, List)} for details. <br>
     */
    public Payment createPayment(String userId, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl, Product product, int quantity) throws Exception {
        List<Product> products = new ArrayList<>(5);
        for (int i = 0; i < quantity; i++) {
            products.add(product);
        }
        return createPayments(userId, paymentProcessor, successUrl, cancelUrl, products)
                .get(0);
    }

    /**
     * Creates a new pending {@link Payment} for each {@link Product}. <br>
     * Redirect your user to {@link Payment#payUrl} to complete the payment. <br>
     * Note that {@link Product}s WITHOUT recurring payments get grouped together <br>
     * and can be paid over the same url. <br>
     * You can listen for payment completion with {@link #onPayment(int, Consumer)}. <br>
     * @param userId Unique identifier of the buying user.
     * @param paymentProcessor The users' desired {@link PaymentProcessor}.
     * @param products List of {@link Product}s the user wants to buy.
     *                Cannot be null, empty, or contain products with different currencies.
     *                 If the user wants the same {@link Product} twice for example, simply add it twice to this list.
     * @param successUrl Redirect the user to this url on a successful checkout.
     * @param cancelUrl Redirect the user to this url on an aborted checkout.
     */
    public List<Payment> createPayments(String userId, PaymentProcessor paymentProcessor, String successUrl, String cancelUrl, List<Product> products) throws Exception {
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
        if(paymentProcessor.equals(PaymentProcessor.STRIPE)){
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
                            (quantity * p.priceInSmallestCurrency), p.currency, true,
                            p.name, null, null, now,
                            null, null, null));
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
                        p.priceInSmallestCurrency, p.currency, true,
                        p.name, session.getUrl(), null, now,
                        null, null, null);
                payments.add(payment);
                database.insertPayment(payment);
            }
        } else if(paymentProcessor.equals(PaymentProcessor.PAYPAL)){
            //TODO
        }
        return null; // TODO
    }

    /**
     * Does not compare payment specific details.
     * @return true if the provided {@link Product}s have different essential information.
     */
    private boolean compareProducts(Product p1, Product p2) {
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
        if (p1.billingType != p2.billingType)
            return true;
        return p1.customBillingIntervallInDays != p2.customBillingIntervallInDays;
    }

    /**
     * Executed when a valid payment was received on a WebHook. <br>
     */
    public void onPayment(int paymentId, Consumer<PaymentEvent> action) {
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
    public void onMissedPayment(Consumer<PaymentEvent> action) {
        synchronized (actionsOnMissedPayment) {
            actionsOnMissedPayment.add(action);
        }
    }

    private void executeMissedPayment(PaymentEvent paymentEvent) {
        synchronized (actionsOnMissedPayment) {
            for (Consumer<PaymentEvent> action : actionsOnMissedPayment) {
                action.accept(paymentEvent);
            }
        }
    }

    public void checkForMissedPayments() throws SQLException {
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
