package com.osiris.payhook;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PayHookDatabase {
    private final Connection databaseConnection;
    public final AtomicInteger paymentsId = new AtomicInteger();

    public PayHookDatabase(Connection databaseConnection, boolean isSandbox) throws SQLException {
        this.databaseConnection = databaseConnection;

        try (Statement stm = databaseConnection.createStatement()) {
            if(isSandbox) stm.executeUpdate("CREATE DATABASE IF NOT EXISTS payhook_sandbox");
            else stm.executeUpdate("CREATE DATABASE IF NOT EXISTS payhook");
            stm.executeUpdate("CREATE TABLE IF NOT EXISTS products" +
                    "(id int NOT NULL PRIMARY KEY, " +
                    ")"); // TODO price, currency, name, description, billingType, customBillingIntervallInDays, paypalProductId
            // TODO stripeProductId, stripePriceId,
            stm.executeUpdate("CREATE TABLE IF NOT EXISTS orders" +
                    "(id int NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    ")"); // TODO price, currency, name, description, billingType, customBillingIntervallInDays,
            // TODO lastPaymentTimestamp, refundTimestamp, cancelTimestamp, payUrl
            stm.executeUpdate("CREATE TABLE IF NOT EXISTS payments" +
                    "(id int NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    ")"); // TODO
        }

        //paymentsId.set(); // TODO
    }

    public void insertProduct(Product product) throws SQLException {
        /* TODO
        try (PreparedStatement stm = databaseConnection.prepareStatement("INSERT INTO products (" +
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
        }*/
    }

    public void updateProduct(Product product) {
        //TODO
    }

    public Product getProductById(int id) {
        return null; //TODO
    }

    public synchronized Payment insertPayment(Payment payment) throws SQLException {
        /*
        TODO
        try (PreparedStatement stm = databaseConnection.prepareStatement("INSERT INTO orders (" +
                "price, currency, name, description," +
                "billingType, customBillingIntervallInDays," +
                "lastPaymentTimestamp," +
                "refundTimestamp, cancelTimestamp, payUrl)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            stm.setLong(1, payment.getPriceInSmallestCurrency());
            stm.setString(2, payment.getCurrency());
            stm.setString(3, payment.getName());
            stm.setString(4, payment.getDescription());
            stm.setInt(5, payment.getBillingType());
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
                "billingType=?, customBillingIntervallInDays=?," +
                "lastPaymentTimestamp=?," +
                "refundTimestamp=?, cancelTimestamp=?, payUrl=?" +
                " WHERE id=?")) {
            stm.setLong(1, payment.getPriceInSmallestCurrency());
            stm.setString(2, payment.getCurrency());
            stm.setString(3, payment.getName());
            stm.setString(4, payment.getDescription());
            stm.setInt(5, payment.getBillingType());
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

    public List<Payment> getPayments() throws SQLException {
        /* TODO
        List<Order> list = new ArrayList<>();
        try (PreparedStatement stm = databaseConnection.prepareStatement("SELECT " +
                "id, payUrl, price, currency, name, description," +
                "billingType, customBillingIntervallInDays," +
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

    public Connection getDatabaseConnection() {
        return databaseConnection;
    }

    public void deleteProductById(int id) {
        //TODO
    }
}
