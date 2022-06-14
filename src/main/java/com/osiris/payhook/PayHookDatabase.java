package com.osiris.payhook;

import com.osiris.sql.SQLTable;
import com.osiris.sql.SQLUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PayHookDatabase {
    private final Connection con;
    public final AtomicInteger paymentsId = new AtomicInteger();
    public SQLTable tableProducts;
    public SQLTable tablePayments;

    public PayHookDatabase(Connection con) throws SQLException {
        this.con = con;
        SQLUtils sql = new SQLUtils();
        sql.initTables(con,
                (tableProducts = sql.table("products",
                        sql.col("charge", "BIGINT NOT NULL"),
                        sql.col("currency", "CHAR(3) NOT NULL"),
                        sql.col("name", "TEXT(65532) NOT NULL"),
                        sql.col("description", "TEXT(65532) NOT NULL"),
                        sql.col("payment_intervall", "INT NOT NULL"),
                        sql.col("paypal_product_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("paypal_plan_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("stripe_product_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("stripe_price_id", "TEXT(65532) DEFAULT NULL"))),
                (tablePayments = sql.table("payments",
                        sql.col("user_id", "TEXT(65532) NOT NULL"),
                        sql.col("charge", "BIGINT NOT NULL"),
                        sql.col("currency", "CHAR(3) NOT NULL"),
                        sql.col("intervall", "INT NOT NULL"),
                        sql.col("url", "TEXT(65532) DEFAULT NULL"),
                        sql.col("product_id", "INT DEFAULT NULL"),
                        sql.col("product_name", "TEXT(65532) DEFAULT NULL"),
                        sql.col("product_quantity", "INT DEFAULT NULL"),
                        sql.col("timestamp_created", "BIGINT DEFAULT NULL"),
                        sql.col("timestamp_expires", "BIGINT DEFAULT NULL"),
                        sql.col("timestamp_authorized", "BIGINT DEFAULT NULL"),
                        sql.col("timestamp_cancelled", "BIGINT DEFAULT NULL"),
                        sql.col("stripe_payment_intent_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("stripe_subscription_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("stripe_charge_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("paypal_order_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("paypal_subscription_id", "TEXT(65532) DEFAULT NULL"),
                        sql.col("paypal_capture_id", "TEXT(65532) DEFAULT NULL"))
                ));
        try (PreparedStatement stm = con.prepareStatement("SELECT id FROM "+tablePayments.name
                + " ORDER BY id DESC LIMIT 1")) {
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                paymentsId.set(rs.getInt(1));
            }
        }
    }

    public PayHookDatabase addProduct(Product product) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(tableProducts.insert+"(" +
                "id, price, currency, name, description," +
                "payment_type, payment_intervall, paypal_product_id, paypal_plan_id," +
                "stripe_product_id, stripe_price_id)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?")) {
            ps.setInt(1, product.id);
            ps.setLong(2, product.charge);
            ps.setString(3, product.currency);
            ps.setString(4, product.name);
            ps.setString(5, product.description);
            ps.setInt(6, product.paymentIntervall.type);
            ps.setInt(7, product.customPaymentIntervall);
            ps.setString(8,product.paypalProductId);
            ps.setString(9,product.paypalPlanId);
            ps.setString(10,product.stripeProductId);
            ps.setString(11,product.stripePriceId);
            ps.executeUpdate();
        }
        return this;
    }

    public PayHookDatabase updateProduct(Product product) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(tableProducts.update +
                " SET price=?, currency=?, name=?, description=?," +
                "payment_type=?, payment_intervall=?," +
                "paypal_product_id=?, paypal_plan_id=?," +
                "stripe_product_id=?, stripe_price_id=?" +
                " WHERE id=?")) {
            ps.setLong(1, product.charge);
            ps.setString(2, product.currency);
            ps.setString(3, product.name);
            ps.setString(4, product.description);
            ps.setInt(5, product.paymentIntervall.type);
            ps.setInt(6, product.customPaymentIntervall);
            ps.setString(7, product.paypalProductId);
            ps.setString(8, product.paypalPlanId);
            ps.setString(9, product.stripeProductId);
            ps.setString(10, product.stripePriceId);
            ps.setInt(11, product.id);
            ps.executeUpdate();
        }
        return this;
    }

    public List<Product> getProducts(String where){

        return this;
    }

    /**
     * Updates the existing product or inserts it.
     */
    public void putProduct(Product product) throws SQLException {
        boolean exists = false;
        try (PreparedStatement stm = con.prepareStatement("SELECT id FROM "+tableProducts.name
                + " WHERE id=?")) {
            stm.setInt(1, product.id);
            ResultSet rs = stm.executeQuery();
            exists = rs.next();
        }
        if(exists) updateProduct(product);
        else addProduct(product);
    }

    public Product getProductById(int id) {
        return null; //TODO
    }

    public synchronized Payment insertPayment(Payment payment) throws SQLException {
        /*
        TODO
        try (PreparedStatement stm = databaseConnection.prepareStatement("INSERT INTO orders (" +
                "price, currency, name, description," +
                "paymentType, customBillingIntervallInDays," +
                "lastPaymentTimestamp," +
                "refundTimestamp, cancelTimestamp, payUrl)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            stm.setLong(1, payment.getPriceInSmallestCurrency());
            stm.setString(2, payment.getCurrency());
            stm.setString(3, payment.getName());
            stm.setString(4, payment.getDescription());
            stm.setInt(5, payment.getpaymentType());
            stm.setInt(6, payment.getCustomBillingIntervallInDays());
            stm.setTimestamp(7, payment.getLastPaymentTimestamp());
            stm.setTimestamp(8, payment.getRefundTimestamp());
            stm.setTimestamp(9, payment.getCancelTimestamp());
            stm.setString(10, payment.getPayUrl());
            stm.executeUpdate();
        }
        try (PreparedStatement stm = databaseConnection.prepareStatement("SELECT LAST_INSERT_ID()")) {
            ResultSet rs = stm.executeQuery();
            rs.next();
            payment.setId(rs.getInt(1));
        }*/
        return payment;
    }

    public Payment updatePayment(Payment payment) throws SQLException {
        /* TODO
        try (PreparedStatement stm = databaseConnection.prepareStatement("UPDATE orders" +
                " SET price=?, currency=?, name=?, description=?," +
                "paymentType=?, customBillingIntervallInDays=?," +
                "lastPaymentTimestamp=?," +
                "refundTimestamp=?, cancelTimestamp=?, payUrl=?" +
                " WHERE id=?")) {
            stm.setLong(1, payment.getPriceInSmallestCurrency());
            stm.setString(2, payment.getCurrency());
            stm.setString(3, payment.getName());
            stm.setString(4, payment.getDescription());
            stm.setInt(5, payment.getpaymentType());
            stm.setInt(6, payment.getCustomBillingIntervallInDays());
            stm.setTimestamp(7, payment.getLastPaymentTimestamp());
            stm.setTimestamp(8, payment.getRefundTimestamp());
            stm.setTimestamp(9, payment.getCancelTimestamp());
            stm.setInt(10, payment.getId());
            stm.setString(11, payment.getPayUrl());
            stm.executeUpdate();
        }*/
        return payment;
    }

    public Payment getPaymentById(int id){
        //TODO
        return null;
    }

    public Payment getPaymentBy(String field, String value){
        //TODO
        return null;
    }

    public List<Payment> getPaymentsById(int id){
        //TODO
        return null;
    }

    public List<Payment> getPayments(String field, String value){
        //TODO
        return null;
    }

    public List<Payment> getPayments() throws SQLException {
        /* TODO
        List<Order> list = new ArrayList<>();
        try (PreparedStatement stm = databaseConnection.prepareStatement("SELECT " +
                "id, payUrl, price, currency, name, description," +
                "paymentType, customBillingIntervallInDays," +
                "lastPaymentTimestamp," +
                "refundTimestamp, cancelTimestamp)" +
                " FROM orders")) {
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Order order = new Order(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getLong(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getInt(7),
                        rs.getInt(8),
                        rs.getTimestamp(9),
                        rs.getTimestamp(10),
                        rs.getTimestamp(11)
                );
                list.add(order);
            }
        }
        return list;*/
        return null;
    }

    public Connection getCon() {
        return con;
    }

    public void deleteProductById(int id) {
        //TODO
    }


    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet and are in the future.
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is bigger than now.
     */
    public List<Payment> getPendingFuturePayments() {
        //TODO
        return null;
    }

    /**
     * @see #getPendingPayments()
     */
    public List<Payment> getPendingFuturePayments(String field, String value) {
        //TODO
        return null;
    }


    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet.
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is smaller than now and {@link Payment#timestampExpires} is bigger than now.
     */
    public List<Payment> getPendingPayments() {
        //TODO
        return null;
    }

    /**
     * @see #getPendingPayments()
     */
    public List<Payment> getPendingPayments(String field, String value) {
        //TODO
        return null;
    }

    /**
     * List of payments that have been authorized/completed/paid.
     * @return list of payments, where {@link Payment#timestampAuthorized} is not null.
     * @see PayHook#paymentAuthorizedEvent
     */
    public List<Payment> getAuthorizedPayments() {
        //TODO
        return null;
    }

    /**
     * @see #getAuthorizedPayments()
     */
    public List<Payment> getAuthorizedPayments(String field, String value) {
        //TODO
        return null;
    }

    /**
     * List of payments that have been cancelled (or expired).
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#paymentCancelledEvent
     */
    public List<Payment> getCancelledPayments() {
        //TODO
        return null;
    }

    /**
     * @see #getCancelledPayments()
     */
    public List<Payment> getCancelledPayments(String field, String value) {
        //TODO
        return null;
    }

    /**
     * List of payments that have been refunded.
     * @return list of payments, where {@link Payment#charge} is smaller than 0.
     */
    public List<Payment> getRefundedPayments() {
        //TODO
        return null;
    }

    /**
     * @see #getRefundedPayments()
     */
    public List<Payment> getRefundedPayments(String field, String value) {
        //TODO
        return null;
    }

}
