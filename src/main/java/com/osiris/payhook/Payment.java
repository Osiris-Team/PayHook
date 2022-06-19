package com.osiris.payhook;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Payment {
    private static final java.sql.Connection con;
    private static final java.util.concurrent.atomic.AtomicInteger idCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    static {
        try {
            con = java.sql.DriverManager.getConnection(Database.url, Database.username, Database.password);
            try (Statement s = con.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS `Payment` (id INT NOT NULL PRIMARY KEY)");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS id INT NOT NULL PRIMARY KEY");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS id INT NOT NULL PRIMARY KEY");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS userId TEXT(65532) NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS userId TEXT(65532) NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS charge BIGINT NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS charge BIGINT NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS currency CHAR(3) NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS currency CHAR(3) NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS intervall INT NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS intervall INT NOT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS url TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS url TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS productId INT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS productId INT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS productName TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS productName TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS productQuantity INT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS productQuantity INT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS timestampCreated BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS timestampCreated BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS timestampExpires BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS timestampExpires BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS timestampAuthorized BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS timestampAuthorized BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS timestampCancelled BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS timestampCancelled BIGINT DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS stripePaymentIntentId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS stripePaymentIntentId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS stripeSubscriptionId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS stripeSubscriptionId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS stripeChargeId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS stripeChargeId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS paypalOrderId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS paypalOrderId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS paypalSubscriptionId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS paypalSubscriptionId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN IF NOT EXISTS paypalCaptureId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Payment` MODIFY IF EXISTS paypalCaptureId TEXT(65532) DEFAULT NULL");
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `Payment` ORDER BY id DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCounter.set(rs.getInt(1));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Database field/value. Not null. <br>
     */
    public int id;
    /**
     * Database field/value. Not null. <br>
     */
    public String userId;
    /**
     * Database field/value. Not null. <br>
     */
    public long charge;
    /**
     * Database field/value. Not null. <br>
     */
    public String currency;
    /**
     * Database field/value. Not null. <br>
     */
    public int intervall;
    /**
     * Database field/value. <br>
     */
    public String url;
    /**
     * Database field/value. <br>
     */
    public int productId;
    /**
     * Database field/value. <br>
     */
    public String productName;
    /**
     * Database field/value. <br>
     */
    public int productQuantity;
    /**
     * Database field/value. <br>
     */
    public long timestampCreated;
    /**
     * Database field/value. <br>
     */
    public long timestampExpires;
    /**
     * Database field/value. <br>
     */
    public long timestampAuthorized;
    /**
     * Database field/value. <br>
     */
    public long timestampCancelled;
    /**
     * Database field/value. <br>
     */
    public String stripePaymentIntentId;
    /**
     * Database field/value. <br>
     */
    public String stripeSubscriptionId;
    /**
     * Database field/value. <br>
     */
    public String stripeChargeId;
    /**
     * Database field/value. <br>
     */
    public String paypalOrderId;
    /**
     * Database field/value. <br>
     */
    public String paypalSubscriptionId;
    /**
     * Database field/value. <br>
     */
    public String paypalCaptureId;
    private Payment() {
    }
    /**
     * Use the static create method instead of this constructor,
     * if you plan to add this object to the database in the future, since
     * that method fetches and sets/reserves the {@link #id}.
     */
    public Payment(int id, String userId, long charge, String currency, int intervall) {
        this.id = id;
        this.userId = userId;
        this.charge = charge;
        this.currency = currency;
        this.intervall = intervall;
    }
    /**
     * Use the static create method instead of this constructor,
     * if you plan to add this object to the database in the future, since
     * that method fetches and sets/reserves the {@link #id}.
     */
    public Payment(int id, String userId, long charge, String currency, int intervall, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, String stripePaymentIntentId, String stripeSubscriptionId, String stripeChargeId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId) {
        this.id = id;
        this.userId = userId;
        this.charge = charge;
        this.currency = currency;
        this.intervall = intervall;
        this.url = url;
        this.productId = productId;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.timestampCreated = timestampCreated;
        this.timestampExpires = timestampExpires;
        this.timestampAuthorized = timestampAuthorized;
        this.timestampCancelled = timestampCancelled;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeChargeId = stripeChargeId;
        this.paypalOrderId = paypalOrderId;
        this.paypalSubscriptionId = paypalSubscriptionId;
        this.paypalCaptureId = paypalCaptureId;
    }

    /**
     * Increments the id and sets it for this object (basically reserves a space in the database).
     *
     * @return object with latest id. Should be added to the database next by you.
     */
    public static Payment create(String userId, long charge, String currency, int intervall) {
        int id = idCounter.incrementAndGet();
        Payment obj = new Payment(id, userId, charge, currency, intervall);
        return obj;
    }

    public static Payment create(String userId, long charge, String currency, int intervall, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, String stripePaymentIntentId, String stripeSubscriptionId, String stripeChargeId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId) {
        int id = idCounter.incrementAndGet();
        Payment obj = new Payment();
        obj.id = id;
        obj.userId = userId;
        obj.charge = charge;
        obj.currency = currency;
        obj.intervall = intervall;
        obj.url = url;
        obj.productId = productId;
        obj.productName = productName;
        obj.productQuantity = productQuantity;
        obj.timestampCreated = timestampCreated;
        obj.timestampExpires = timestampExpires;
        obj.timestampAuthorized = timestampAuthorized;
        obj.timestampCancelled = timestampCancelled;
        obj.stripePaymentIntentId = stripePaymentIntentId;
        obj.stripeSubscriptionId = stripeSubscriptionId;
        obj.stripeChargeId = stripeChargeId;
        obj.paypalOrderId = paypalOrderId;
        obj.paypalSubscriptionId = paypalSubscriptionId;
        obj.paypalCaptureId = paypalCaptureId;
        return obj;
    }

    /**
     * @return a list containing all objects in this table.
     */
    public static List<Payment> get() throws Exception {
        return get(null);
    }

    /**
     * @return object with the provided id.
     * @throws Exception on SQL issues, or if there is no object with the provided id in this table.
     */
    public static Payment get(int id) throws Exception {
        return get("id = " + id).get(0);
    }

    /**
     * @return a list containing only objects that match the provided SQL WHERE statement.
     * if that statement is null, returns all the contents of this table.
     */
    public static List<Payment> get(String where) throws Exception {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id,userId,charge,currency,intervall,url,productId,productName,productQuantity,timestampCreated,timestampExpires,timestampAuthorized,timestampCancelled,stripePaymentIntentId,stripeSubscriptionId,stripeChargeId,paypalOrderId,paypalSubscriptionId,paypalCaptureId" +
                        " FROM `Payment`" +
                        (where != null ? ("WHERE " + where) : ""))) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Payment obj = new Payment();
                list.add(obj);
                obj.id = rs.getInt(1);
                obj.userId = rs.getString(2);
                obj.charge = rs.getLong(3);
                obj.currency = rs.getString(4);
                obj.intervall = rs.getInt(5);
                obj.url = rs.getString(6);
                obj.productId = rs.getInt(7);
                obj.productName = rs.getString(8);
                obj.productQuantity = rs.getInt(9);
                obj.timestampCreated = rs.getLong(10);
                obj.timestampExpires = rs.getLong(11);
                obj.timestampAuthorized = rs.getLong(12);
                obj.timestampCancelled = rs.getLong(13);
                obj.stripePaymentIntentId = rs.getString(14);
                obj.stripeSubscriptionId = rs.getString(15);
                obj.stripeChargeId = rs.getString(16);
                obj.paypalOrderId = rs.getString(17);
                obj.paypalSubscriptionId = rs.getString(18);
                obj.paypalCaptureId = rs.getString(19);
            }
        }
        return list;
    }

    /**
     * Searches the provided object in the database (by its id),
     * and updates all its fields.
     *
     * @throws Exception when failed to find by id.
     */
    public static void update(Payment obj) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE `Payment` SET id=?,userId=?,charge=?,currency=?,intervall=?,url=?,productId=?,productName=?,productQuantity=?,timestampCreated=?,timestampExpires=?,timestampAuthorized=?,timestampCancelled=?,stripePaymentIntentId=?,stripeSubscriptionId=?,stripeChargeId=?,paypalOrderId=?,paypalSubscriptionId=?,paypalCaptureId=?")) {
            ps.setInt(1, obj.id);
            ps.setString(2, obj.userId);
            ps.setLong(3, obj.charge);
            ps.setString(4, obj.currency);
            ps.setInt(5, obj.intervall);
            ps.setString(6, obj.url);
            ps.setInt(7, obj.productId);
            ps.setString(8, obj.productName);
            ps.setInt(9, obj.productQuantity);
            ps.setLong(10, obj.timestampCreated);
            ps.setLong(11, obj.timestampExpires);
            ps.setLong(12, obj.timestampAuthorized);
            ps.setLong(13, obj.timestampCancelled);
            ps.setString(14, obj.stripePaymentIntentId);
            ps.setString(15, obj.stripeSubscriptionId);
            ps.setString(16, obj.stripeChargeId);
            ps.setString(17, obj.paypalOrderId);
            ps.setString(18, obj.paypalSubscriptionId);
            ps.setString(19, obj.paypalCaptureId);
            ps.executeUpdate();
        }
    }

    /**
     * Adds the provided object to the database (note that the id is not checked for duplicates).
     */
    public static void add(Payment obj) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO `Payment` (id,userId,charge,currency,intervall,url,productId,productName,productQuantity,timestampCreated,timestampExpires,timestampAuthorized,timestampCancelled,stripePaymentIntentId,stripeSubscriptionId,stripeChargeId,paypalOrderId,paypalSubscriptionId,paypalCaptureId) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, obj.id);
            ps.setString(2, obj.userId);
            ps.setLong(3, obj.charge);
            ps.setString(4, obj.currency);
            ps.setInt(5, obj.intervall);
            ps.setString(6, obj.url);
            ps.setInt(7, obj.productId);
            ps.setString(8, obj.productName);
            ps.setInt(9, obj.productQuantity);
            ps.setLong(10, obj.timestampCreated);
            ps.setLong(11, obj.timestampExpires);
            ps.setLong(12, obj.timestampAuthorized);
            ps.setLong(13, obj.timestampCancelled);
            ps.setString(14, obj.stripePaymentIntentId);
            ps.setString(15, obj.stripeSubscriptionId);
            ps.setString(16, obj.stripeChargeId);
            ps.setString(17, obj.paypalOrderId);
            ps.setString(18, obj.paypalSubscriptionId);
            ps.setString(19, obj.paypalCaptureId);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes the provided object from the database.
     */
    public static void remove(Payment obj) throws Exception {
        remove("id = " + obj.id);
    }

    /**
     * Deletes the objects that are found by the provided SQL WHERE statement, from the database.
     */
    public static void remove(String where) throws Exception {
        java.util.Objects.requireNonNull(where);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM `Payment` WHERE " + where)) {
            ps.executeUpdate();
        }
    }

    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet and are in the future.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is bigger than now.
     */
    public static List<Payment> getPendingFuturePayments() throws Exception {
        return getPendingFuturePayments(null);
    }

    /*
    ADDITIONAL CODE:
     */

    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet and are in the future.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is bigger than now.
     */
    public static List<Payment> getPendingFuturePayments(String where) throws Exception {
        return get("timestampAuthorized = 0 AND timestampCancelled = 0 AND timestampCreated > " + System.currentTimeMillis() +
                (where != null ? " AND " + where : ""));
    }

    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is smaller than now and {@link Payment#timestampExpires} is bigger than now.
     */
    public static List<Payment> getPendingPayments() throws Exception {
        return getPendingPayments(null);
    }

    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is smaller than now and {@link Payment#timestampExpires} is bigger than now.
     */
    public static List<Payment> getPendingPayments(String where) throws Exception {
        long now = System.currentTimeMillis();
        return get("timestampAuthorized = 0 AND timestampCancelled = 0 AND timestampCreated < " + now
                + " AND timestampCreated > " + now + (where != null ? " AND " + where : ""));
    }

    /**
     * List of payments that have been authorized/completed/paid.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is not null.
     * @see PayHook#paymentAuthorizedEvent
     */
    public static List<Payment> getAuthorizedPayments() throws Exception {
        return getAuthorizedPayments(null);
    }

    /**
     * List of payments that have been authorized/completed/paid.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is not null.
     * @see PayHook#paymentAuthorizedEvent
     */
    public static List<Payment> getAuthorizedPayments(String where) throws Exception {
        return get("timestampAuthorized != 0" + (where != null ? " AND " + where : ""));
    }

    /**
     * List of payments that have been cancelled (or expired).
     *
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#paymentCancelledEvent
     */
    public static List<Payment> getCancelledPayments() throws Exception {
        return getCancelledPayments(null);
    }

    /**
     * List of payments that have been cancelled (or expired).
     *
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#paymentCancelledEvent
     */
    public static List<Payment> getCancelledPayments(String where) throws Exception {
        return get("timestampCancelled != 0" + (where != null ? " AND " + where : ""));
    }

    /**
     * List of payments that have been refunded.
     *
     * @return list of payments, where {@link Payment#charge} is smaller than 0.
     */
    public static List<Payment> getRefundedPayments() throws Exception {
        return getRefundedPayments(null);
    }

    /**
     * List of payments that have been refunded.
     *
     * @return list of payments, where {@link Payment#charge} is smaller than 0.
     */
    public static List<Payment> getRefundedPayments(String where) throws Exception {
        return get("charge < 0" + (where != null ? " AND " + where : ""));
    }

    public Payment clone() {
        return new Payment(this.id, this.userId, this.charge, this.currency, this.intervall, this.url, this.productId, this.productName, this.productQuantity, this.timestampCreated, this.timestampExpires, this.timestampAuthorized, this.timestampCancelled, this.stripePaymentIntentId, this.stripeSubscriptionId, this.stripeChargeId, this.paypalOrderId, this.paypalSubscriptionId, this.paypalCaptureId);
    }

    public PaymentProcessor getPaymentProcessor() {
        if (isPayPalSupported()) return PaymentProcessor.PAYPAL;
        else if (isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
        // TODO ADD NEW PROCESSORS
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", charge=" + charge +
                ", currency='" + currency + '\'' +
                ", url='" + url + '\'' +
                ", intervall=" + intervall +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", productQuantity=" + productQuantity +
                ", paymentProcessor=" + getPaymentProcessor() +
                '}';
    }

    public boolean isPayPalSupported() {
        return paypalSubscriptionId != null || paypalOrderId != null || paypalCaptureId != null;
    }

    public boolean isStripeSupported() {
        return stripePaymentIntentId != null || stripeSubscriptionId != null;
    }

    public long getUrlTimeoutMs() {
        PaymentProcessor paymentProcessor = getPaymentProcessor();
        if (paymentProcessor == PaymentProcessor.PAYPAL) return PayHook.paypalUrlTimeoutMs;
        else if (paymentProcessor == PaymentProcessor.STRIPE) return PayHook.stripeUrlTimeoutMs;
        else throw new IllegalArgumentException("Unknown/Invalid payment processor: " + paymentProcessor);
        // TODO ADD NEW PROCESSORS
    }

    /**
     * Must be a recurring payment, otherwise just returns -1. <br>
     * Note that this will always return the difference between the last two (latest and future) payments
     * for this subscription and ignore this {@link Payment} object (also returns -1 when there is no future payment).
     *
     * @return the time left (in milliseconds) until the next due payment.
     * Thus, you get a negative value, if the due payment date was already exceeded, which usually means
     * that the subscription was cancelled.
     * @throws NullPointerException when the future {@link Payment#timestampCreated} is null.
     */
    public long getMsLeftUntilNextPayment() throws Exception {
        if (!isRecurring()) return -1;
        long now = System.currentTimeMillis();
        List<Payment> futurePayments;
        if (isPayPalSupported())
            futurePayments = Payment.getPendingFuturePayments("paypalSubscriptionId = " + paypalSubscriptionId);
        else if (isStripeSupported())
            futurePayments = Payment.getPendingFuturePayments("stripeSubscriptionId = " + stripeSubscriptionId);
        else throw new IllegalArgumentException("Unknown/Invalid payment processor: " + getPaymentProcessor());
        // TODO ADD NEW PROCESSORS
        if (futurePayments.isEmpty()) return -1;
        return Objects.requireNonNull(futurePayments.get(0)).timestampCreated - now;
    }


    public boolean isPending() {
        return timestampAuthorized == 0 && timestampCancelled == 0;
    }

    public boolean isRecurring() {
        return intervall != 0;
    }

    public boolean isCancelled() {
        return timestampCancelled != 0;
    }

    public boolean isAuthorized() {
        return timestampAuthorized != 0;
    }

    public boolean isRefund() {
        return charge < 0;
    }

    public boolean isFree() {
        return charge == 0;
    }

    /**
     * Helper class to set the payments billing intervall in days.
     */
    public static class Intervall {
        /**
         * One time payment. Type 0.
         */
        public static final int NONE = 0;
        /**
         * Recurring payment every month (exactly 30 days). Type 1.
         */
        public static final int MONTHLY = 30;
        /**
         * Recurring payment every 3 months (exactly 90 days). Type 2.
         */
        public static final int TRI_MONTHLY = 90;
        /**
         * Recurring payment every 6 months (exactly 180 days). Type 3.
         */
        public static final int HALF_YEARLY = 180;
        /**
         * Recurring payment every 12 months (exactly 360 days). Type 4.
         */
        public static final int YEARLY = 360;

        public static long toHours(long days) {
            return days * 24;
        }

        public static long toMilliseconds(long days) {
            return toHours(days) * 3600000;
        }
    }
}
