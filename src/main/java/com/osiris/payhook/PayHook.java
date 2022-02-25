package com.osiris.payhook;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.payhook.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.PayPalWebHookEventValidator;
import com.osiris.payhook.paypal.PaypalJsonUtils;
import com.osiris.payhook.paypal.PaypalWebhookEvent;
import com.osiris.payhook.paypal.codec.binary.Base64;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Still work in progress. <br>
 * Release planned in v3.0 <br>
 */
public class PayHook {
    public final PayHookDatabase database;
    private Thread commandLineThread;
    private final List<Consumer<Event>> actionsOnMissedPayment = new ArrayList<>();

    // Stripe specific:
    public boolean isStripeSandbox;
    private String stripeSecretKey;

    // PayPal specific:
    public boolean isPaypalSandbox;
    private String paypalAPIUrl;
    private String paypalClientId;
    private String paypalClientSecret;
    private String paypalBase64EncodedCredentials;
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

    /**
     * Executed when the due payment hasn't been received yet. <br>
     * Note that the user is given one extra day to pay. <br>
     * Also note that this is executed every hour, until the order is cancelled. <br>
     */
    public void onMissedPayment(Consumer<Event> action) {
        synchronized (actionsOnMissedPayment) {
            actionsOnMissedPayment.add(action);
        }
    }

    private void executeMissedPayment(Event event) {
        synchronized (actionsOnMissedPayment) {
            for (Consumer<Event> action : actionsOnMissedPayment) {
                action.accept(event);
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
                        executeMissedPayment(new Event(o));

                } else if (o.isBillingInterval3Months()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month3)
                        executeMissedPayment(new Event(o));
                } else if (o.isBillingInterval6Months()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month6)
                        executeMissedPayment(new Event(o));
                } else if (o.isBillingInterval12Months()) {
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month12)
                        executeMissedPayment(new Event(o));
                } else { // Custom payment intervall
                    long custom = o.getCustomBillingIntervallInDays() * 86400000L; // xdays multiplied with 1 day as millisecond
                    if ((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > custom)
                        executeMissedPayment(new Event(o));
                }
            }
        }
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
                        if (command.equals("exit")){
                            exit = true;
                        } else if(command.equals("help") || command.equals("h")){
                            out.println("Available commands:");
                            // TODO add commands like:
                            // payments <days> // Prints all received payments from the last <days> (if not provided 30 days is the default)
                        } else{
                            out.println("Unknown command. Enter 'help' or 'h' for a list of all commands.");
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
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2ApiContext = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        } else {
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
                              int billingType, int customBillingIntervallInDays) throws StripeException, SQLException, PayPalRESTException {
        Converter converter = new Converter();
        // TODO also link webhook urls
        Product newProduct = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                null, null);
        Product dbProduct = database.getProductById(id);
        if (dbProduct == null) {
            dbProduct = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                    null, null);
            if (paypalClientId != null && paypalClientSecret != null) {
                // Note that PayPal doesn't store products, but only plans, in its databases.
                // Thus, products don't need to get updated, except plans
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = converter.toPayPalPlan(dbProduct);
                    plan.create(paypalV1ApiContext);
                    dbProduct.paypalProductId = plan.getId();
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
     * Creates and adds a new {@link Order} to the database, with the selected payment processor. <br>
     * Redirect your user to {@link Order#getPayUrl()} to pay and complete the order. <br>
     * You can listen for payment completion with {@link Order#onPaymentReceived(Consumer)}. <br>
     * @param products The product the user wants to buy.
     * @param quantity The quantity of the product.
     * @param successUrl Redirect the user to this url on a successful checkout.
     * @param cancelUrl Redirect the user to this url on an aborted checkout.
     */
    public Order createOrder(PaymentProcessor paymentProcessor, String successUrl, String cancelUrl, Product... products) throws Exception {
        Objects.requireNonNull(products);
        if (products.length == 0) throw new Exception("Products array cannot be empty!");
        String currency = products[0].currency;
        long totalPriceInSmallestCurrency = 0;
        Product[] productsCopy = Arrays.copyOf(products, products.length);
        // Calculate total price, make sure all products have the same currency
        // and support Stripe.
        for (Product pCopy : productsCopy) {
            for (Product p : products) {
                if (!p.currency.equals(pCopy.currency)) {
                    throw new Exception("All provided products must have the same currency!");
                }
            }
            if(paymentProcessor.equals(PaymentProcessor.STRIPE) && !pCopy.isStripeSupported())
                throw new Exception("One of the provided products does not support Stripe.");
            totalPriceInSmallestCurrency += pCopy.priceInSmallestCurrency;
        }
        if(paymentProcessor.equals(PaymentProcessor.STRIPE)){
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl);
            for (Product p :
                    products) {
                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setName(p.name)
                                .setDescription(p.description)
                                .setQuantity(1L)
                                // Provide the exact Price ID (e.g. pr_1234) of the product you want to sell
                                .setPrice("{{" + p.stripePriceId + "}}")
                                .build());
            }
            Session session = Session.create(paramsBuilder.build());
            return database.putOrder(session.getUrl(), totalPriceInSmallestCurrency, currency, products);
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
        if (p1.billingType != p2.billingType)
            return true;
        return p1.customBillingIntervallInDays != p2.customBillingIntervallInDays;
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
}
