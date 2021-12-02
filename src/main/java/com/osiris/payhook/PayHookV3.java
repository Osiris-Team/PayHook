package com.osiris.payhook;

import com.paypal.api.payments.Plan;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalEnvironment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PayHookV3 {
    private final PayHookDatabase payHookDatabase;
    private String paypalClientId;
    private String paypalClientSecret;
    private String stripeSecretKey;
    private String paypalAPIUrl;
    private boolean isStripeSandbox;
    private boolean isPaypalSandbox;

    private APIContext paypalV1ApiContext;
    private PayPalEnvironment paypalV2Enviornment;

    private final List<Consumer<Event>> actionsOnMissedPayment = new ArrayList<>();

    /**
     * PayHook makes payments easy. Workflow: <br>
     * 1. Set the credentials of your selected payment processors. <br>
     * 2. Create/Update a or multiple {@link Product}s. <br>
     * 3. Create an {@link Order} with a or multiple {@link Product}s, for the selected payment processor. <br>
     * 4. Redirect user to complete the payment. <br>
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
    public PayHookV3(String databaseUrl, String databaseUsername, String databasePassword) throws SQLException {
        payHookDatabase = new PayHookDatabase(DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword));
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
        List<Order> orders = payHookDatabase.getOrders();
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

    public void initPayPal(boolean isSandbox, String clientId, String clientSecret) {
        this.paypalClientId = clientId;
        this.paypalClientSecret = clientSecret;
        this.isPaypalSandbox = isSandbox;

        if (isSandbox) {
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2Enviornment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        } else {
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "live");
            paypalV2Enviornment = new PayPalEnvironment.Live(clientId, clientSecret);
        }
    }

    public void initStripe(boolean isSandbox, String secretKey) {
        this.stripeSecretKey = secretKey;
        this.isStripeSandbox = isSandbox;
        Stripe.apiKey = secretKey;
    }

    public String getPaypalClientId() {
        return paypalClientId;
    }

    public String getPaypalClientSecret() {
        return paypalClientSecret;
    }

    public String getStripeSecretKey() {
        return stripeSecretKey;
    }

    public String getPaypalAPIUrl() {
        return paypalAPIUrl;
    }

    public void setPaypalAPIUrl(String paypalAPIUrl) {
        this.paypalAPIUrl = paypalAPIUrl;
    }

    /**
     * Creates and adds a new {@link Order} to the database, with the selected payment processor. <br>
     * Redirect your user to {@link Order#getPayUrl()} to pay and complete the order. <br>
     * You can listen for payment completion with {@link Order#onPaymentReceived(Consumer)}. <br>
     * @param product The product the user wants to buy.
     * @param quantity The quantity of the product.
     * @param successUrl Redirect the user to this url on a successful checkout.
     * @param cancelUrl Redirect the user to this url on an aborted checkout.
     */
    public Order createStripeOrder(Product product, int quantity, String successUrl, String cancelUrl) throws StripeException, SQLException {
        long priceInSmallestCurrency = product.getPriceInSmallestCurrency();
        String currency = product.getCurrency();
        String name = product.getName();
        String description = product.getDescription();
        int billingType = product.getBillingType();
        int customBillingIntervallInDays = product.getCustomBillingIntervallInDays();
        Session session = Session.create(SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                // Provide the exact Price ID (e.g. pr_1234) of the product you want to sell
                                .setPrice("{{PRICE_ID}}")
                                .build())
                .build());
        Order order = new Order(
                0, session.getUrl(), priceInSmallestCurrency, currency, name,
                description, billingType, customBillingIntervallInDays,
                null, null, null);
        payHookDatabase.insertOrder(order);
        return order;
    }

    /**
     * Call this method after setting the credentials for your payment processors. <br>
     * If the provided id doesn't exist in the database, it gets created/inserted. <br>
     * If the provided id exists in the database and the new provided values differ from the values in the database, it gets updated. <br>
     * The above also happens for the {@link Product}s saved on the servers of the payment processors. <br>
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
    public Product createProduct(int id, long priceInSmallestCurrency,
                                 String currency, String name, String description,
                                 int billingType, int customBillingIntervallInDays) throws StripeException, SQLException, PayPalRESTException {
        Product newProduct = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                null, null);
        Product dbProduct = payHookDatabase.getProductById(id);
        if (dbProduct == null) {
            dbProduct = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                    null, null);
            if (paypalClientId != null && paypalClientSecret != null) {
                // Note that PayPal doesn't store products, but only plans, in its databases.
                // Thus, products don't need to get updated, except plans
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = new Plan().create(paypalV1ApiContext);
                    dbProduct.setPaypalProductId(plan.getId());
                }
            }
            if (stripeSecretKey != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("name", name);
                params.put("description", description);
                params.put("livemode", isStripeSandbox);
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.create(params);
                dbProduct.setStripeProductId(stripeProduct.getId());

                Map<String, Object> paramsPrice = new HashMap<>();
                paramsPrice.put("currency", currency);
                paramsPrice.put("product", stripeProduct.getId());
                if (dbProduct.isRecurring()){
                    Price.Recurring stripeRecurring = new Price.Recurring();
                    if (dbProduct.isCustomBillingInterval()){
                        stripeRecurring.setInterval("day");
                        stripeRecurring.setIntervalCount((long) customBillingIntervallInDays);
                    } else{
                        stripeRecurring.setInterval("month");
                        if (dbProduct.isBillingInterval1Month())
                            stripeRecurring.setIntervalCount(1L);
                        else if (dbProduct.isBillingInterval3Months())
                            stripeRecurring.setIntervalCount(3L);
                        else if (dbProduct.isBillingInterval6Months())
                            stripeRecurring.setIntervalCount(6L);
                        else if (dbProduct.isBillingInterval12Months())
                            stripeRecurring.setIntervalCount(12L);
                    }
                    paramsPrice.put("recurring", stripeRecurring);
                }
                com.stripe.model.Price stripePrice = com.stripe.model.Price.create(paramsPrice);
                dbProduct.setStripePriceId(stripePrice.getId());
            }
            payHookDatabase.insertProduct(dbProduct);
        }

        newProduct.setPaypalProductId(dbProduct.getPaypalProductId());
        newProduct.setStripeProductId(dbProduct.getStripeProductId());

        if (hasProductChanges(newProduct, dbProduct)) {
            payHookDatabase.updateProduct(newProduct);

            if (paypalClientId != null && paypalClientSecret != null && dbProduct.getPaypalProductId() != null) {
                // Note that PayPal doesn't store products, but only plans, in its databases.
                // Thus, products don't need to get updated, except plans
                if (dbProduct.isRecurring()) {
                    com.paypal.api.payments.Plan plan = Plan.get(paypalV1ApiContext, dbProduct.getPaypalProductId());
                    plan.update(); // TODO
                }
            }
            if (stripeSecretKey != null && dbProduct.getStripeProductId() != null) {
                com.stripe.model.Product stripeProduct = com.stripe.model.Product.retrieve(dbProduct.getStripeProductId());
                Map<String, Object> params = new HashMap<>();
                params.put("name", name);
                params.put("description", description);
                params.put("livemode", isStripeSandbox);
                stripeProduct.update(params);
            }
        }
        return newProduct;
    }

    private boolean hasProductChanges(Product newProduct, Product dbProduct) {
        if (newProduct.getId() != dbProduct.getId())
            return true;
        if (newProduct.getPriceInSmallestCurrency() != dbProduct.getPriceInSmallestCurrency())
            return true;
        if (!newProduct.getCurrency().equals(dbProduct.getCurrency()))
            return true;
        if (!newProduct.getName().equals(dbProduct.getName()))
            return true;
        if (!newProduct.getDescription().equals(dbProduct.getDescription()))
            return true;
        if (newProduct.getBillingType() != dbProduct.getBillingType())
            return true;
        return newProduct.getCustomBillingIntervallInDays() != dbProduct.getCustomBillingIntervallInDays();
    }

}
