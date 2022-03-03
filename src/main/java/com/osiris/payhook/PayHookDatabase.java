package com.osiris.payhook;

import com.osiris.ljdb.SQLTable;
import com.osiris.ljdb.SQLUtils;

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

    public PayHookDatabase(String dbName, Connection con) throws SQLException {
        this.con = con;
        SQLUtils sql = new SQLUtils();
        sql.initDatabase(con, dbName,
                (tableProducts = sql.table("products",
                        sql.col("id", "INT NOT NULL PRIMARY KEY"),
                        sql.col("price", "LONG NOT NULL"),
                        sql.col("currency", "CHAR(3) NOT NULL"),
                        sql.col("name", "VARCHAR DEFAULT NULL"),
                        sql.col("description", "VARCHAR DEFAULT NULL"),
                        sql.col("payment_type", "TINYINT NOT NULL"),
                        sql.col("payment_intervall", "INT NOT NULL"),
                        sql.col("braintree_product_id", "VARCHAR DEFAULT NULL"),
                        sql.col("braintree_plan_id", "VARCHAR DEFAULT NULL"),
                        sql.col("stripe_product_id", "VARCHAR DEFAULT NULL"),
                        sql.col("stripe_price_id", "VARCHAR DEFAULT NULL"))),
                (tablePayments = sql.table("payments",
                        sql.col("id", "INT NOT NULL AUTO_INCREMENT PRIMARY KEY"),
                        sql.col("timestamp", "TIMESTAMP NOT NULL"),
                        sql.col("daily_visits", "LONG NOT NULL"),
                        sql.col("daily_users", "INT NOT NULL")))
        );
        try (PreparedStatement stm = con.prepareStatement("SELECT id FROM "+tablePayments.name
                + " ORDER BY id DESC LIMIT 1")) {
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                paymentsId.set(rs.getInt(1));
            }
        }
    }

    /**
     * Updates the existing product or inserts it.
     */
    public void putProduct(Product product) throws SQLException {
        boolean exists = false;
        try (PreparedStatement stm = con.prepareStatement("SELECT id FROM "+tablePayments.name
                + " WHERE id=?")) {
            stm.setInt(1, product.id);
            ResultSet rs = stm.executeQuery();
            exists = rs.next();
        }
        // TODO below
        if(exists)
            try (PreparedStatement stm = con.prepareStatement(tablePayments.update +
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
            }
        else
            try (PreparedStatement stm = con.prepareStatement(tableProducts.insert+"(" +
                    "id, price, currency, name, description," +
                    "paymentType, customBillingIntervallInDays)" +
                    " VALUES (?,?,?,?,?,?,?)")) {
                stm.setInt(1, product.id);
                stm.setLong(2, product.priceInSmallestCurrency);
                stm.setString(3, product.currency);
                stm.setString(4, product.name);
                stm.setString(5, product.description);
                stm.setInt(6, product.paymentType.type);
                stm.setInt(7, product.customPaymentIntervall);
                stm.executeUpdate();
            }
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
}
