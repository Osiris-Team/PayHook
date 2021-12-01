package com.osiris.payhook;

import com.paypal.api.payments.Plan;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalEnvironment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
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

    private List<Consumer<Event>> actionsOnMissedPayment = new ArrayList<>();

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
            try{
                while(true){
                    Thread.sleep(600000); // 1h
                    checkForMissedPayments();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void checkForMissedPayments() throws SQLException {
        List<Order> orders = payHookDatabase.getOrders();
        long now     = System.currentTimeMillis();
        long month   = 2629800000L;
        long month3  = 7889400000L;
        long month6  = 15778800000L;
        long month12 = 31557600000L;
        for (Order o :
                orders) {
            if (o.isRecurring()){
                if (o.isBillingInterval1Month()){
                    if((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month)
                        executeMissedPayment(new Event(o));

                }else if(o.isBillingInterval3Months()){
                    if((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month3)
                        executeMissedPayment(new Event(o));
                }
                else if(o.isBillingInterval6Months()){
                    if((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month6)
                        executeMissedPayment(new Event(o));
                }
                else if(o.isBillingInterval12Months()){
                    if((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > month12)
                        executeMissedPayment(new Event(o));
                }
                else { // Custom payment intervall
                    long custom = o.getCustomBillingIntervallInDays() * 86400000L; // xdays multiplied with 1 day as millisecond
                    if((now - o.getLastPaymentTimestamp().toInstant().toEpochMilli()) > custom)
                        executeMissedPayment(new Event(o));
                }
            }
        }
    }

    public void setPaypalCredentials(boolean isSandbox, String clientId, String clientSecret) {
        this.paypalClientId = clientId;
        this.paypalClientSecret = clientSecret;
        this.isPaypalSandbox = isSandbox;

        if (isSandbox){
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "sandbox");
            paypalV2Enviornment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
        }
        else{
            paypalV1ApiContext = new APIContext(clientId, clientSecret, "live");
            paypalV2Enviornment = new PayPalEnvironment.Live(clientId, clientSecret);
        }
    }

    public void setStripeCredentials(boolean isSandbox, String secretKey) {
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
     * Creates and adds a new {@link Order} to the database, with the selected payment processor.
     *
     * @param priceInSmallestCurrency      <br>
     *                                     E.g., 100 cents to charge $1.00 or 100 to charge ¥100, a zero-decimal currency.
     *                                     The amount value supports up to eight digits (e.g., a value of 99999999 for a USD charge of $999,999.99).
     * @param currency                     <br>
     *                                     Three-letter <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO currency code</a>,
     *                                     in lowercase. Must be a <a href="https://stripe.com/docs/currencies">supported currency</a>.
     * @param name                         <br>
     *                                     The name of the product.
     * @param description                  <br>
     *                                     The products' description.
     * @param billingType                  <br>
     *                                     Value between 0 and 5: <br>
     *                                     0 = one time payment <br>
     *                                     1 = recurring payment every month <br>
     *                                     2 = recurring payment every 3 months <br>
     *                                     3 = recurring payment every 6 months <br>
     *                                     4 = recurring payment every 12 months <br>
     *                                     5 = recurring payment with a custom intervall <br>
     * @param customBillingIntervallInDays <br>
     *                                     The custom billing intervall in days. Note that billingType must be set to 5 for this to have affect.
     * @throws StripeException
     * @throws SQLException
     */
    public Order createStripeOrder(long priceInSmallestCurrency, String currency,
                                   String name, String description, int billingType,
                                   int customBillingIntervallInDays, String successUrl, String cancelUrl) throws StripeException, SQLException {
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
     * If the provided id exists in the database, its values get updated. <br>
     * The above also happens for the {@link Product}s saved on the servers of the payment processors. <br>
     */
    public Product createProduct(int id, long priceInSmallestCurrency,
                                 String currency, String name, String description,
                                 int billingType, int customBillingIntervallInDays,
                                 String paypalProductId, String stripeProductId) throws StripeException, SQLException, PayPalRESTException {
        Product product = getProductById(id);
        if (product==null){
            product = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                    paypalProductId, stripeProductId);
            payHookDatabase.insertProduct(product);
        }

        if (paypalClientId!=null && paypalClientSecret!=null){
            // Note that PayPal doesn't store products, but only plans, in its databases.
            // Thus, products don't need to get updated, except plans
            if (product.isRecurring()){
                if (paypalProductId==null){ // Create new plan
                    com.paypal.api.payments.Plan plan = new Plan().create(paypalV1ApiContext);
                    product.setPaypalProductId(plan.getId());
                } else{ // Update existing plan
                    com.paypal.api.payments.Plan plan = Plan.get(paypalV1ApiContext, product.getPaypalProductId());
                    plan.update(); // TODO
                }
            }



        }
        // Create/Update Stripe product
        if (stripeSecretKey!=null){
            com.stripe.model.Product stripeProduct = null;
            try{
                stripeProduct = com.stripe.model.Product.retrieve(product.getStripeProductId());
            } catch (Exception e) {
            }
            Map<String, Object> params = new HashMap<>();
            params.put("name", name);
            params.put("description", description);
            params.put("livemode", isStripeSandbox);
            if (stripeProduct==null){
                stripeProduct = com.stripe.model.Product.create(params);
            } else{
                stripeProduct.update(params);
            }

        }
        // TODO add to database
        // TODO add to payment processors

        return new Product();
    }



    public void onMissedPayment(Consumer<Event> action){
        synchronized (actionsOnMissedPayment){
            actionsOnMissedPayment.add(action);
        }
    }

    private void executeMissedPayment(Event event){
        synchronized (actionsOnMissedPayment){
            for (Consumer<Event> action : actionsOnMissedPayment){
                action.accept(event);
            }
        }
    }

}
