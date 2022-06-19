package com.osiris.payhook;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Product{
    private static java.sql.Connection con;
    private static java.util.concurrent.atomic.AtomicInteger idCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    static {
        try{
            con = java.sql.DriverManager.getConnection(Database.url, Database.username, Database.password);
            try (Statement s = con.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS `Product` (id INT NOT NULL PRIMARY KEY)");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS id INT NOT NULL PRIMARY KEY");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS id INT NOT NULL PRIMARY KEY");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS charge BIGINT NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS charge BIGINT NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS currency CHAR(3) NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS currency CHAR(3) NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS name TEXT(65532) NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS name TEXT(65532) NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS description TEXT(65532) NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS description TEXT(65532) NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS paymentIntervall INT NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS paymentIntervall INT NOT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS paypalProductId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS paypalProductId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS paypalPlanId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS paypalPlanId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS stripeProductId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS stripeProductId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` ADD COLUMN IF NOT EXISTS stripePriceId TEXT(65532) DEFAULT NULL");
                s.executeUpdate("ALTER TABLE `Product` MODIFY IF EXISTS stripePriceId TEXT(65532) DEFAULT NULL");
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `Product` ORDER BY id DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCounter.set(rs.getInt(1));
            }
        }
        catch(Exception e){ throw new RuntimeException(e); }
    }
    private Product(){}
    /**
     Use the static create method instead of this constructor,
     if you plan to add this object to the database in the future, since
     that method fetches and sets/reserves the {@link #id}.
     */
    public Product (int id, long charge, String currency, String name, String description, int paymentIntervall){
        this.id = id;this.charge = charge;this.currency = currency;this.name = name;this.description = description;this.paymentIntervall = paymentIntervall;
    }
    /**
     Use the static create method instead of this constructor,
     if you plan to add this object to the database in the future, since
     that method fetches and sets/reserves the {@link #id}.
     */
    public Product (int id, long charge, String currency, String name, String description, int paymentIntervall, String paypalProductId, String paypalPlanId, String stripeProductId, String stripePriceId){
        this.id = id;this.charge = charge;this.currency = currency;this.name = name;this.description = description;this.paymentIntervall = paymentIntervall;this.paypalProductId = paypalProductId;this.paypalPlanId = paypalPlanId;this.stripeProductId = stripeProductId;this.stripePriceId = stripePriceId;
    }
    /**
     Database field/value. Not null. <br>
     */
    public int id;
    /**
     Database field/value. Not null. <br>
     */
    public long charge;
    /**
     Database field/value. Not null. <br>
     */
    public String currency;
    /**
     Database field/value. Not null. <br>
     */
    public String name;
    /**
     Database field/value. Not null. <br>
     */
    public String description;
    /**
     Database field/value. Not null. <br>
     */
    public int paymentIntervall;
    /**
     Database field/value. <br>
     */
    public String paypalProductId;
    /**
     Database field/value. <br>
     */
    public String paypalPlanId;
    /**
     Database field/value. <br>
     */
    public String stripeProductId;
    /**
     Database field/value. <br>
     */
    public String stripePriceId;
    /**
     Increments the id and sets it for this object (basically reserves a space in the database).
     @return object with latest id. Should be added to the database next by you.
     */
    public static Product create( long charge, String currency, String name, String description, int paymentIntervall) {
        int id = idCounter.incrementAndGet();
        Product obj = new Product(id, charge, currency, name, description, paymentIntervall);
        return obj;
    }

    public static Product create( long charge, String currency, String name, String description, int paymentIntervall, String paypalProductId, String paypalPlanId, String stripeProductId, String stripePriceId) {
        int id = idCounter.incrementAndGet();
        Product obj = new Product();
        obj.id=id; obj.charge=charge; obj.currency=currency; obj.name=name; obj.description=description; obj.paymentIntervall=paymentIntervall; obj.paypalProductId=paypalProductId; obj.paypalPlanId=paypalPlanId; obj.stripeProductId=stripeProductId; obj.stripePriceId=stripePriceId;
        return obj;
    }

    /**
     @return a list containing all objects in this table.
     */
    public static List<Product> get() throws Exception {return get(null);}
    /**
     @return object with the provided id.
     @throws Exception on SQL issues, or if there is no object with the provided id in this table.
     */
    public static Product get(int id) throws Exception {
        return get("id = "+id).get(0);
    }
    /**
     @return a list containing only objects that match the provided SQL WHERE statement.
     if that statement is null, returns all the contents of this table.
     */
    public static List<Product> get(String where) throws Exception {
        List<Product> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id,charge,currency,name,description,paymentIntervall,paypalProductId,paypalPlanId,stripeProductId,stripePriceId" +
                        " FROM `Product`" +
                        (where != null ? ("WHERE "+where) : ""))) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Product obj = new Product();
                list.add(obj);
                obj.id = rs.getInt(1);
                obj.charge = rs.getLong(2);
                obj.currency = rs.getString(3);
                obj.name = rs.getString(4);
                obj.description = rs.getString(5);
                obj.paymentIntervall = rs.getInt(6);
                obj.paypalProductId = rs.getString(7);
                obj.paypalPlanId = rs.getString(8);
                obj.stripeProductId = rs.getString(9);
                obj.stripePriceId = rs.getString(10);
            }
        }
        return list;
    }

    /**
     Searches the provided object in the database (by its id),
     and updates all its fields.
     @throws Exception when failed to find by id.
     */
    public static void update(Product obj) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE `Product` SET id=?,charge=?,currency=?,name=?,description=?,paymentIntervall=?,paypalProductId=?,paypalPlanId=?,stripeProductId=?,stripePriceId=?")) {
            ps.setInt(1, obj.id);
            ps.setLong(2, obj.charge);
            ps.setString(3, obj.currency);
            ps.setString(4, obj.name);
            ps.setString(5, obj.description);
            ps.setInt(6, obj.paymentIntervall);
            ps.setString(7, obj.paypalProductId);
            ps.setString(8, obj.paypalPlanId);
            ps.setString(9, obj.stripeProductId);
            ps.setString(10, obj.stripePriceId);
            ps.executeUpdate();
        }
    }

    /**
     Adds the provided object to the database (note that the id is not checked for duplicates).
     */
    public static void add(Product obj) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO `Product` (id,charge,currency,name,description,paymentIntervall,paypalProductId,paypalPlanId,stripeProductId,stripePriceId) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, obj.id);
            ps.setLong(2, obj.charge);
            ps.setString(3, obj.currency);
            ps.setString(4, obj.name);
            ps.setString(5, obj.description);
            ps.setInt(6, obj.paymentIntervall);
            ps.setString(7, obj.paypalProductId);
            ps.setString(8, obj.paypalPlanId);
            ps.setString(9, obj.stripeProductId);
            ps.setString(10, obj.stripePriceId);
            ps.executeUpdate();
        }
    }

    /**
     Deletes the provided object from the database.
     */
    public static void remove(Product obj) throws Exception {
        remove("id = "+obj.id);
    }
    /**
     Deletes the objects that are found by the provided SQL WHERE statement, from the database.
     */
    public static void remove(String where) throws Exception {
        java.util.Objects.requireNonNull(where);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM `Product` WHERE "+where)) {
            ps.executeUpdate();
        }
    }

    public Product clone(){
        return new Product(this.id,this.charge,this.currency,this.name,this.description,this.paymentIntervall,this.paypalProductId,this.paypalPlanId,this.stripeProductId,this.stripePriceId);
    }


    /*
     * ADDITIONAL/CUSTOM CODE:
     */

    public boolean isPayPalSupported(){
        return paypalProductId != null;
    }

    public boolean isBraintreeSupported(){
        return false; // TODO
    }

    public boolean isStripeSupported(){
        return stripeProductId != null && stripePriceId != null;
    }

    public boolean isRecurring() { // For example a subscription
        return paymentIntervall != 0;
    }

    public String getFormattedPrice() {
        return charge + " " + currency;
    }
}
