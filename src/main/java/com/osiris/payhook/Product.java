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
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN charge BIGINT NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN charge BIGINT NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN currency CHAR(3) NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN currency CHAR(3) NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN name TEXT(65532) NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN name TEXT(65532) NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN description TEXT(65532) NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN description TEXT(65532) NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN paymentIntervall INT NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN paymentIntervall INT NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN paypalProductId TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN paypalProductId TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN paypalPlanId TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN paypalPlanId TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN stripeProductId TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN stripeProductId TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Product` ADD COLUMN stripePriceId TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Product` MODIFY COLUMN stripePriceId TEXT(65532) DEFAULT NULL");
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `Product` ORDER BY id DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCounter.set(rs.getInt(1) + 1);
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
     Creates and returns an object that can be added to this table.
     Increments the id (thread-safe) and sets it for this object (basically reserves a space in the database).
     Note that the parameters of this method represent "NOT NULL" fields in the table and thus should not be null.
     Also note that this method will NOT add the object to the table.
     */
    public static Product create( long charge, String currency, String name, String description, int paymentIntervall) {
        int id = idCounter.getAndIncrement();
        Product obj = new Product(id, charge, currency, name, description, paymentIntervall);
        return obj;
    }

    /**
     Creates and returns an object that can be added to this table.
     Increments the id (thread-safe) and sets it for this object (basically reserves a space in the database).
     Note that this method will NOT add the object to the table.
     */
    public static Product create( long charge, String currency, String name, String description, int paymentIntervall, String paypalProductId, String paypalPlanId, String stripeProductId, String stripePriceId) throws Exception {
        int id = idCounter.getAndIncrement();
        Product obj = new Product();
        obj.id=id; obj.charge=charge; obj.currency=currency; obj.name=name; obj.description=description; obj.paymentIntervall=paymentIntervall; obj.paypalProductId=paypalProductId; obj.paypalPlanId=paypalPlanId; obj.stripeProductId=stripeProductId; obj.stripePriceId=stripePriceId;
        return obj;
    }

    /**
     Convenience method for creating and directly adding a new object to the table.
     Note that the parameters of this method represent "NOT NULL" fields in the table and thus should not be null.
     */
    public static Product createAndAdd( long charge, String currency, String name, String description, int paymentIntervall) throws Exception {
        int id = idCounter.getAndIncrement();
        Product obj = new Product(id, charge, currency, name, description, paymentIntervall);
        add(obj);
        return obj;
    }

    /**
     Convenience method for creating and directly adding a new object to the table.
     */
    public static Product createAndAdd( long charge, String currency, String name, String description, int paymentIntervall, String paypalProductId, String paypalPlanId, String stripeProductId, String stripePriceId) throws Exception {
        int id = idCounter.getAndIncrement();
        Product obj = new Product();
        obj.id=id; obj.charge=charge; obj.currency=currency; obj.name=name; obj.description=description; obj.paymentIntervall=paymentIntervall; obj.paypalProductId=paypalProductId; obj.paypalPlanId=paypalPlanId; obj.stripeProductId=stripeProductId; obj.stripePriceId=stripePriceId;
        add(obj);
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
     Example: <br>
     get("username=? AND age=?", "Peter", 33);  <br>
     @param where can be null. Your SQL WHERE statement (without the leading WHERE).
     @param whereValues can be null. Your SQL WHERE statement values to set for '?'.
     @return a list containing only objects that match the provided SQL WHERE statement.
     if that statement is null, returns all the contents of this table.
     */
    public static List<Product> get(String where, Object... whereValues) throws Exception {
        List<Product> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id,charge,currency,name,description,paymentIntervall,paypalProductId,paypalPlanId,stripeProductId,stripePriceId" +
                        " FROM `Product`" +
                        (where != null ? ("WHERE "+where) : ""))) {
            if(where!=null && whereValues!=null)
                for (int i = 0; i < whereValues.length; i++) {
                    Object val = whereValues[i];
                    ps.setObject(i+1, val);
                }
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
     Example: <br>
     remove("username=?", "Peter"); <br>
     Deletes the objects that are found by the provided SQL WHERE statement, from the database.
     @param whereValues can be null. Your SQL WHERE statement values to set for '?'.
     */
    public static void remove(String where, Object... whereValues) throws Exception {
        java.util.Objects.requireNonNull(where);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM `Product` WHERE "+where)) {
            if(whereValues != null)
                for (int i = 0; i < whereValues.length; i++) {
                    Object val = whereValues[i];
                    ps.setObject(i+1, val);
                }
            ps.executeUpdate();
        }
    }

    public Product clone(){
        return new Product(this.id,this.charge,this.currency,this.name,this.description,this.paymentIntervall,this.paypalProductId,this.paypalPlanId,this.stripeProductId,this.stripePriceId);
    }
    public String toPrintString(){
        return  ""+"id="+this.id+" "+"charge="+this.charge+" "+"currency="+this.currency+" "+"name="+this.name+" "+"description="+this.description+" "+"paymentIntervall="+this.paymentIntervall+" "+"paypalProductId="+this.paypalProductId+" "+"paypalPlanId="+this.paypalPlanId+" "+"stripeProductId="+this.stripeProductId+" "+"stripePriceId="+this.stripePriceId+" ";
    }
    public static WHERE whereId() {
        return new WHERE("id");
    }
    public static WHERE whereCharge() {
        return new WHERE("charge");
    }
    public static WHERE whereCurrency() {
        return new WHERE("currency");
    }
    public static WHERE whereName() {
        return new WHERE("name");
    }
    public static WHERE whereDescription() {
        return new WHERE("description");
    }
    public static WHERE wherePaymentIntervall() {
        return new WHERE("paymentIntervall");
    }
    public static WHERE wherePaypalProductId() {
        return new WHERE("paypalProductId");
    }
    public static WHERE wherePaypalPlanId() {
        return new WHERE("paypalPlanId");
    }
    public static WHERE whereStripeProductId() {
        return new WHERE("stripeProductId");
    }
    public static WHERE whereStripePriceId() {
        return new WHERE("stripePriceId");
    }
    public static class WHERE {
        /**
         * Remember to prepend WHERE on the final SQL statement.
         * This is not done by this class due to performance reasons. <p>
         * <p>
         * Note that it excepts the generated SQL string to be used by a {@link java.sql.PreparedStatement}
         * to protect against SQL-Injection. <p>
         * <p>
         * Also note that the SQL query gets optimized by the database automatically,
         * thus It's recommended to make queries as readable as possible and
         * not worry that much about performance.
         */
        public StringBuilder sqlBuilder = new StringBuilder();
        public StringBuilder orderByBuilder = new StringBuilder();
        public StringBuilder limitBuilder = new StringBuilder();
        List<Object> whereObjects = new ArrayList<>();
        private final String columnName;
        public WHERE(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Executes the generated SQL statement
         * and returns a list of objects matching the query.
         */
        public List<Product> get() throws Exception {
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                return Product.get(sqlBuilder.toString()+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                return Product.get(sqlBuilder.toString()+orderBy+limitBuilder.toString(), (Object[]) null);
        }

        /**
         * Executes the generated SQL statement
         * and removes the objects matching the query.
         */
        public void remove() throws Exception {
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                Product.remove(sqlBuilder.toString()+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                Product.remove(sqlBuilder.toString()+orderBy+limitBuilder.toString(), (Object[]) null);
        }

        /**
         * AND (...) <br>
         */
        public WHERE and(WHERE where) {
            String sql = where.sqlBuilder.toString();
            if(!sql.isEmpty()) {
                sqlBuilder.append("AND (").append(sql).append(") ");
                whereObjects.addAll(where.whereObjects);
            }
            orderByBuilder.append(where.orderByBuilder.toString());
            return this;
        }

        /**
         * OR (...) <br>
         */
        public WHERE or(WHERE where) {
            String sql = where.sqlBuilder.toString();
            if(!sql.isEmpty()) {
                sqlBuilder.append("OR (").append(sql).append(") ");
                whereObjects.addAll(where.whereObjects);
            }
            orderByBuilder.append(where.orderByBuilder.toString());
            return this;
        }

        /**
         * columnName = ? <br>
         */
        public WHERE is(Object obj) {
            sqlBuilder.append(columnName).append(" = ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName IN (?,?,...) <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_in.asp">https://www.w3schools.com/mysql/mysql_in.asp</a>
         */
        public WHERE is(Object... objects) {
            String s = "";
            for (Object obj : objects) {
                s += "?,";
                whereObjects.add(obj);
            }
            s = s.substring(0, s.length() - 1); // Remove last ,
            sqlBuilder.append(columnName).append(" IN (" + s + ") ");
            return this;
        }

        /**
         * columnName <> ? <br>
         */
        public WHERE isNot(Object obj) {
            sqlBuilder.append(columnName).append(" <> ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName IS NULL <br>
         */
        public WHERE isNull() {
            sqlBuilder.append(columnName).append(" IS NULL ");
            return this;
        }

        /**
         * columnName IS NOT NULL <br>
         */
        public WHERE isNotNull() {
            sqlBuilder.append(columnName).append(" IS NOT NULL ");
            return this;
        }

        /**
         * columnName LIKE ? <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE like(Object obj) {
            sqlBuilder.append(columnName).append(" LIKE ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName NOT LIKE ? <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE notLike(Object obj) {
            sqlBuilder.append(columnName).append(" NOT LIKE ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName > ? <br>
         */
        public WHERE biggerThan(Object obj) {
            sqlBuilder.append(columnName).append(" > ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName < ? <br>
         */
        public WHERE smallerThan(Object obj) {
            sqlBuilder.append(columnName).append(" < ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName >= ? <br>
         */
        public WHERE biggerOrEqual(Object obj) {
            sqlBuilder.append(columnName).append(" >= ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName <= ? <br>
         */
        public WHERE smallerOrEqual(Object obj) {
            sqlBuilder.append(columnName).append(" <= ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName BETWEEN ? AND ? <br>
         */
        public WHERE between(Object obj1, Object obj2) {
            sqlBuilder.append(columnName).append(" BETWEEN ? AND ? ");
            whereObjects.add(obj1);
            whereObjects.add(obj2);
            return this;
        }

        /**
         * columnName ASC, <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE smallestFirst() {
            orderByBuilder.append(columnName + " ASC, ");
            return this;
        }

        /**
         * columnName DESC, <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE biggestFirst() {
            orderByBuilder.append(columnName + " DESC, ");
            return this;
        }

        /**
         * LIMIT number <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_limit.asp">https://www.w3schools.com/mysql/mysql_limit.asp</a>
         */
        public WHERE limit(int num) {
            limitBuilder.append("LIMIT ").append(num + " ");
            return this;
        }

    }


    /*
     * ADDITIONAL/CUSTOM CODE:
     */

    public boolean isPayPalSupported() {
        return paypalProductId != null;
    }

    public boolean isBraintreeSupported() {
        return false; // TODO
    }

    public boolean isStripeSupported() {
        return stripeProductId != null && stripePriceId != null;
    }

    public boolean isRecurring() { // For example a subscription
        return paymentIntervall != 0;
    }

    public String getFormattedPrice() {
        return charge + " " + currency;
    }
}
