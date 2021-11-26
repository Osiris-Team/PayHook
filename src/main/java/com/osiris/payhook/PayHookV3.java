package com.osiris.payhook;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class PayHookV3 {
    private final Connection databaseConnection;
    private String paypalClientId;
    private String paypalClientSecret;
    private String stripeSecretKey;
    private String paypalAPIUrl;
    private boolean isStripeSandbox;
    private boolean isPaypalSandbox;

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
     */
    public PayHookV3(String databaseUrl, String databaseUsername, String databasePassword) throws SQLException {
        databaseConnection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
        try (Statement stm = databaseConnection.createStatement()) {
            stm.executeUpdate("CREATE DATABASE IF NOT EXISTS payhook");
            stm.executeUpdate("CREATE TABLE IF NOT EXISTS orders" +
                    "(id int NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    ")"); // TODO
        }
    }

    /**
     * Inserts the provided order into the database and also updates its id.
     */
    public synchronized void insertOrder(Order order) throws SQLException {
        try (PreparedStatement stm = databaseConnection.prepareStatement("INSERT INTO orders (" +
                "price, currency, name, description," +
                "billingType, customBillingIntervallInDays," +
                "lastPaymentTimestamp," +
                "refundTimestamp, cancelTimestamp)" +
                " VALUES (?,?,?,?,?,?,?,?,?)")) {
            stm.setLong(1, order.getPriceInSmallestCurrency());
            stm.setString(2, order.getCurrency());
            stm.setString(3, order.getName());
            stm.setString(4, order.getDescription());
            stm.setInt(5, order.getBillingType());
            stm.setInt(6, order.getCustomBillingIntervallInDays());
            stm.setTimestamp(7, order.getLastPaymentTimestamp());
            stm.setTimestamp(8, order.getRefundTimestamp());
            stm.setTimestamp(9, order.getCancelTimestamp());
            stm.executeUpdate();
        }
        try (PreparedStatement stm = databaseConnection.prepareStatement("SELECT LAST_INSERT_ID()")) {
            ResultSet rs = stm.executeQuery();
            rs.next();
            order.setId(rs.getInt(1));
        }
    }

    public void updateOrder(Order order) throws SQLException {
        try (PreparedStatement stm = databaseConnection.prepareStatement("UPDATE orders" +
                " SET price=?, currency=?, name=?, description=?," +
                "billingType=?, customBillingIntervallInDays=?," +
                "lastPaymentTimestamp=?," +
                "refundTimestamp=?, cancelTimestamp=?" +
                " WHERE id=?")) {
            stm.setLong(1, order.getPriceInSmallestCurrency());
            stm.setString(2, order.getCurrency());
            stm.setString(3, order.getName());
            stm.setString(4, order.getDescription());
            stm.setInt(5, order.getBillingType());
            stm.setInt(6, order.getCustomBillingIntervallInDays());
            stm.setTimestamp(7, order.getLastPaymentTimestamp());
            stm.setTimestamp(8, order.getRefundTimestamp());
            stm.setTimestamp(9, order.getCancelTimestamp());
            stm.setInt(10, order.getId());
            stm.executeUpdate();
        }
    }

    public Connection getDatabaseConnection() {
        return databaseConnection;
    }

    public void setPaypalCredentials(boolean isSandbox, String clientId, String clientSecret) {
        this.paypalClientId = clientId;
        this.paypalClientSecret = clientSecret;
        this.isPaypalSandbox = isSandbox;
        if (isSandbox)
            paypalAPIUrl = "https://api-m.sandbox.paypal.com/v1";
        else
            paypalAPIUrl = "https://api-m.paypal.com/v1";
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
        insertOrder(order);
        return order;
    }

    /**
     * Call this method after setting the credentials for your payment processors. <br>
     * If the provided id doesn't exist in the database, it gets created/inserted. <br>
     * If the provided id exists in the database, its values get updated. <br>
     * The above also happens for the {@link Product}s saved on the servers of the payment processors. <br>
     */
    public Product putProduct(int id, long priceInSmallestCurrency,
                              String currency, String name, String description,
                              int billingType, int customBillingIntervallInDays,
                              String paypalProductId, String stripeProductId) throws StripeException, SQLException {
        Product product = getProductById(id);
        if (product==null){ // Create the product in database and on payment processors
            product = new Product(id, priceInSmallestCurrency, currency, name, description, billingType, customBillingIntervallInDays,
                    paypalProductId, stripeProductId);
            insertProduct(product);
        }

        if (paypalClientId!=null && paypalClientSecret!=null){

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

    private void insertProduct(Product product) throws SQLException {
        try (PreparedStatement stm = databaseConnection.prepareStatement("INSERT INTO orders (" +
                "id, price, currency, name, description," +
                "billingType, customBillingIntervallInDays)" +
                " VALUES (?,?,?,?,?,?,?)")) {
            stm.setInt(1, product.getId());
            stm.setLong(2, product.getPriceInSmallestCurrency());
            stm.setString(3, product.getCurrency());
            stm.setString(4, product.getName());
            stm.setString(5, product.getDescription());
            stm.setInt(6, product.getBillingType());
            stm.setInt(7, product.getCustomBillingIntervallInDays());
            stm.executeUpdate();
        }
    }

}
