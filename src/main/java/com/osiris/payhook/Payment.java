package com.osiris.payhook;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 Generated class by <a href="https://github.com/Osiris-Team/jSQL-Gen">jSQL-Gen</a>
 that contains static methods for fetching/updating data from the "Payment" table.
 A single object/instance of this class represents a single row in the table
 and data can be accessed via its public fields. <p>
 Its not recommended to modify this class but it should be OK to add new methods to it.
 If modifications are really needed create a pull request directly to jSQL-Gen instead.
 */
public class Payment{
    private static java.sql.Connection con;
    private static java.util.concurrent.atomic.AtomicInteger idCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    static {
        try{
            con = java.sql.DriverManager.getConnection(Database.url, Database.username, Database.password);
            try (Statement s = con.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS `Payment` (`id` INT NOT NULL PRIMARY KEY)");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `userId` TEXT(65532) NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `userId` TEXT(65532) NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `charge` BIGINT NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `charge` BIGINT NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `currency` CHAR(3) NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `currency` CHAR(3) NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `interval` INT NOT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `interval` INT NOT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `url` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `url` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `productId` INT DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `productId` INT DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `productName` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `productName` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `productQuantity` INT DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `productQuantity` INT DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `timestampCreated` BIGINT DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `timestampCreated` BIGINT DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `timestampExpires` BIGINT DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `timestampExpires` BIGINT DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `timestampAuthorized` BIGINT DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `timestampAuthorized` BIGINT DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `timestampCancelled` BIGINT DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `timestampCancelled` BIGINT DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `stripePaymentIntentId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `stripePaymentIntentId` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `stripeSubscriptionId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `stripeSubscriptionId` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `stripeChargeId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `stripeChargeId` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `paypalOrderId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `paypalOrderId` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `paypalSubscriptionId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `paypalSubscriptionId` TEXT(65532) DEFAULT NULL");
                try{s.executeUpdate("ALTER TABLE `Payment` ADD COLUMN `paypalCaptureId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
                s.executeUpdate("ALTER TABLE `Payment` MODIFY COLUMN `paypalCaptureId` TEXT(65532) DEFAULT NULL");
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `Payment` ORDER BY id DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCounter.set(rs.getInt(1) + 1);
            }
        }
        catch(Exception e){ throw new RuntimeException(e); }
    }
    private Payment(){}
    /**
     Use the static create method instead of this constructor,
     if you plan to add this object to the database in the future, since
     that method fetches and sets/reserves the {@link #id}.
     */
    public Payment (int id, String userId, long charge, String currency, int interval){
        this.id = id;this.userId = userId;this.charge = charge;this.currency = currency;this.interval = interval;
    }
    /**
     Use the static create method instead of this constructor,
     if you plan to add this object to the database in the future, since
     that method fetches and sets/reserves the {@link #id}.
     */
    public Payment (int id, String userId, long charge, String currency, int interval, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, String stripePaymentIntentId, String stripeSubscriptionId, String stripeChargeId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId){
        this.id = id;this.userId = userId;this.charge = charge;this.currency = currency;this.interval = interval;this.url = url;this.productId = productId;this.productName = productName;this.productQuantity = productQuantity;this.timestampCreated = timestampCreated;this.timestampExpires = timestampExpires;this.timestampAuthorized = timestampAuthorized;this.timestampCancelled = timestampCancelled;this.stripePaymentIntentId = stripePaymentIntentId;this.stripeSubscriptionId = stripeSubscriptionId;this.stripeChargeId = stripeChargeId;this.paypalOrderId = paypalOrderId;this.paypalSubscriptionId = paypalSubscriptionId;this.paypalCaptureId = paypalCaptureId;
    }
    /**
     Database field/value. Not null. <br>
     */
    public int id;
    /**
     Database field/value. Not null. <br>
     */
    public String userId;
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
    public int interval;
    /**
     Database field/value. <br>
     */
    public String url;
    /**
     Database field/value. <br>
     */
    public int productId;
    /**
     Database field/value. <br>
     */
    public String productName;
    /**
     Database field/value. <br>
     */
    public int productQuantity;
    /**
     Database field/value. <br>
     */
    public long timestampCreated;
    /**
     Database field/value. <br>
     */
    public long timestampExpires;
    /**
     Database field/value. <br>
     */
    public long timestampAuthorized;
    /**
     Database field/value. <br>
     */
    public long timestampCancelled;
    /**
     Database field/value. <br>
     */
    public String stripePaymentIntentId;
    /**
     Database field/value. <br>
     */
    public String stripeSubscriptionId;
    /**
     Database field/value. <br>
     */
    public String stripeChargeId;
    /**
     Database field/value. <br>
     */
    public String paypalOrderId;
    /**
     Database field/value. <br>
     */
    public String paypalSubscriptionId;
    /**
     Database field/value. <br>
     */
    public String paypalCaptureId;
    /**
     Creates and returns an object that can be added to this table.
     Increments the id (thread-safe) and sets it for this object (basically reserves a space in the database).
     Note that the parameters of this method represent "NOT NULL" fields in the table and thus should not be null.
     Also note that this method will NOT add the object to the table.
     */
    public static Payment create( String userId, long charge, String currency, int interval) {
        int id = idCounter.getAndIncrement();
        Payment obj = new Payment(id, userId, charge, currency, interval);
        return obj;
    }

    /**
     Creates and returns an object that can be added to this table.
     Increments the id (thread-safe) and sets it for this object (basically reserves a space in the database).
     Note that this method will NOT add the object to the table.
     */
    public static Payment create( String userId, long charge, String currency, int interval, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, String stripePaymentIntentId, String stripeSubscriptionId, String stripeChargeId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId)  {
        int id = idCounter.getAndIncrement();
        Payment obj = new Payment();
        obj.id=id; obj.userId=userId; obj.charge=charge; obj.currency=currency; obj.interval=interval; obj.url=url; obj.productId=productId; obj.productName=productName; obj.productQuantity=productQuantity; obj.timestampCreated=timestampCreated; obj.timestampExpires=timestampExpires; obj.timestampAuthorized=timestampAuthorized; obj.timestampCancelled=timestampCancelled; obj.stripePaymentIntentId=stripePaymentIntentId; obj.stripeSubscriptionId=stripeSubscriptionId; obj.stripeChargeId=stripeChargeId; obj.paypalOrderId=paypalOrderId; obj.paypalSubscriptionId=paypalSubscriptionId; obj.paypalCaptureId=paypalCaptureId;
        return obj;
    }

    /**
     Convenience method for creating and directly adding a new object to the table.
     Note that the parameters of this method represent "NOT NULL" fields in the table and thus should not be null.
     */
    public static Payment createAndAdd( String userId, long charge, String currency, int interval)  {
        int id = idCounter.getAndIncrement();
        Payment obj = new Payment(id, userId, charge, currency, interval);
        add(obj);
        return obj;
    }

    /**
     Convenience method for creating and directly adding a new object to the table.
     */
    public static Payment createAndAdd( String userId, long charge, String currency, int interval, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, String stripePaymentIntentId, String stripeSubscriptionId, String stripeChargeId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId)  {
        int id = idCounter.getAndIncrement();
        Payment obj = new Payment();
        obj.id=id; obj.userId=userId; obj.charge=charge; obj.currency=currency; obj.interval=interval; obj.url=url; obj.productId=productId; obj.productName=productName; obj.productQuantity=productQuantity; obj.timestampCreated=timestampCreated; obj.timestampExpires=timestampExpires; obj.timestampAuthorized=timestampAuthorized; obj.timestampCancelled=timestampCancelled; obj.stripePaymentIntentId=stripePaymentIntentId; obj.stripeSubscriptionId=stripeSubscriptionId; obj.stripeChargeId=stripeChargeId; obj.paypalOrderId=paypalOrderId; obj.paypalSubscriptionId=paypalSubscriptionId; obj.paypalCaptureId=paypalCaptureId;
        add(obj);
        return obj;
    }

    /**
     @return a list containing all objects in this table.
     */
    public static List<Payment> get()  {return get(null);}
    /**
     @return object with the provided id or null if there is no object with the provided id in this table.
     @throws Exception on SQL issues.
     */
    public static Payment get(int id)  {
        try{
            return get("id = "+id).get(0);
        }catch(IndexOutOfBoundsException ignored){}
        catch(Exception e){throw new RuntimeException(e);}
        return null;
    }
    /**
     Example: <br>
     get("username=? AND age=?", "Peter", 33);  <br>
     @param where can be null. Your SQL WHERE statement (without the leading WHERE).
     @param whereValues can be null. Your SQL WHERE statement values to set for '?'.
     @return a list containing only objects that match the provided SQL WHERE statement (no matches = empty list).
     if that statement is null, returns all the contents of this table.
     */
    public static List<Payment> get(String where, Object... whereValues)  {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT `id`,`userId`,`charge`,`currency`,`interval`,`url`,`productId`,`productName`,`productQuantity`,`timestampCreated`,`timestampExpires`,`timestampAuthorized`,`timestampCancelled`,`stripePaymentIntentId`,`stripeSubscriptionId`,`stripeChargeId`,`paypalOrderId`,`paypalSubscriptionId`,`paypalCaptureId`" +
                        " FROM `Payment`" +
                        (where != null ? ("WHERE "+where) : ""))) {
            if(where!=null && whereValues!=null)
                for (int i = 0; i < whereValues.length; i++) {
                    Object val = whereValues[i];
                    ps.setObject(i+1, val);
                }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Payment obj = new Payment();
                list.add(obj);
                obj.id = rs.getInt(1);
                obj.userId = rs.getString(2);
                obj.charge = rs.getLong(3);
                obj.currency = rs.getString(4);
                obj.interval = rs.getInt(5);
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
        }catch(Exception e){throw new RuntimeException(e);}
        return list;
    }

    /**
     Searches the provided object in the database (by its id),
     and updates all its fields.
     @throws Exception when failed to find by id or other SQL issues.
     */
    public static void update(Payment obj)  {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE `Payment` SET `id`=?,`userId`=?,`charge`=?,`currency`=?,`interval`=?,`url`=?,`productId`=?,`productName`=?,`productQuantity`=?,`timestampCreated`=?,`timestampExpires`=?,`timestampAuthorized`=?,`timestampCancelled`=?,`stripePaymentIntentId`=?,`stripeSubscriptionId`=?,`stripeChargeId`=?,`paypalOrderId`=?,`paypalSubscriptionId`=?,`paypalCaptureId`=? WHERE id="+obj.id)) {
            ps.setInt(1, obj.id);
            ps.setString(2, obj.userId);
            ps.setLong(3, obj.charge);
            ps.setString(4, obj.currency);
            ps.setInt(5, obj.interval);
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
        }catch(Exception e){throw new RuntimeException(e);}
    }

    /**
     Adds the provided object to the database (note that the id is not checked for duplicates).
     */
    public static void add(Payment obj)  {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO `Payment` (`id`,`userId`,`charge`,`currency`,`interval`,`url`,`productId`,`productName`,`productQuantity`,`timestampCreated`,`timestampExpires`,`timestampAuthorized`,`timestampCancelled`,`stripePaymentIntentId`,`stripeSubscriptionId`,`stripeChargeId`,`paypalOrderId`,`paypalSubscriptionId`,`paypalCaptureId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, obj.id);
            ps.setString(2, obj.userId);
            ps.setLong(3, obj.charge);
            ps.setString(4, obj.currency);
            ps.setInt(5, obj.interval);
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
        }catch(Exception e){throw new RuntimeException(e);}
    }

    /**
     Deletes the provided object from the database.
     */
    public static void remove(Payment obj)  {
        remove("id = "+obj.id);
    }
    /**
     Example: <br>
     remove("username=?", "Peter"); <br>
     Deletes the objects that are found by the provided SQL WHERE statement, from the database.
     @param whereValues can be null. Your SQL WHERE statement values to set for '?'.
     */
    public static void remove(String where, Object... whereValues)  {
        java.util.Objects.requireNonNull(where);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM `Payment` WHERE "+where)) {
            if(whereValues != null)
                for (int i = 0; i < whereValues.length; i++) {
                    Object val = whereValues[i];
                    ps.setObject(i+1, val);
                }
            ps.executeUpdate();
        }catch(Exception e){throw new RuntimeException(e);}
    }

    public Payment clone(){
        return new Payment(this.id,this.userId,this.charge,this.currency,this.interval,this.url,this.productId,this.productName,this.productQuantity,this.timestampCreated,this.timestampExpires,this.timestampAuthorized,this.timestampCancelled,this.stripePaymentIntentId,this.stripeSubscriptionId,this.stripeChargeId,this.paypalOrderId,this.paypalSubscriptionId,this.paypalCaptureId);
    }
    public String toPrintString(){
        return  ""+"id="+this.id+" "+"userId="+this.userId+" "+"charge="+this.charge+" "+"currency="+this.currency+" "+"interval="+this.interval+" "+"url="+this.url+" "+"productId="+this.productId+" "+"productName="+this.productName+" "+"productQuantity="+this.productQuantity+" "+"timestampCreated="+this.timestampCreated+" "+"timestampExpires="+this.timestampExpires+" "+"timestampAuthorized="+this.timestampAuthorized+" "+"timestampCancelled="+this.timestampCancelled+" "+"stripePaymentIntentId="+this.stripePaymentIntentId+" "+"stripeSubscriptionId="+this.stripeSubscriptionId+" "+"stripeChargeId="+this.stripeChargeId+" "+"paypalOrderId="+this.paypalOrderId+" "+"paypalSubscriptionId="+this.paypalSubscriptionId+" "+"paypalCaptureId="+this.paypalCaptureId+" ";
    }
    public static WHERE whereId() {
        return new WHERE("`id`");
    }
    public static WHERE whereUserId() {
        return new WHERE("`userId`");
    }
    public static WHERE whereCharge() {
        return new WHERE("`charge`");
    }
    public static WHERE whereCurrency() {
        return new WHERE("`currency`");
    }
    public static WHERE whereInterval() {
        return new WHERE("`interval`");
    }
    public static WHERE whereUrl() {
        return new WHERE("`url`");
    }
    public static WHERE whereProductId() {
        return new WHERE("`productId`");
    }
    public static WHERE whereProductName() {
        return new WHERE("`productName`");
    }
    public static WHERE whereProductQuantity() {
        return new WHERE("`productQuantity`");
    }
    public static WHERE whereTimestampCreated() {
        return new WHERE("`timestampCreated`");
    }
    public static WHERE whereTimestampExpires() {
        return new WHERE("`timestampExpires`");
    }
    public static WHERE whereTimestampAuthorized() {
        return new WHERE("`timestampAuthorized`");
    }
    public static WHERE whereTimestampCancelled() {
        return new WHERE("`timestampCancelled`");
    }
    public static WHERE whereStripePaymentIntentId() {
        return new WHERE("`stripePaymentIntentId`");
    }
    public static WHERE whereStripeSubscriptionId() {
        return new WHERE("`stripeSubscriptionId`");
    }
    public static WHERE whereStripeChargeId() {
        return new WHERE("`stripeChargeId`");
    }
    public static WHERE wherePaypalOrderId() {
        return new WHERE("`paypalOrderId`");
    }
    public static WHERE wherePaypalSubscriptionId() {
        return new WHERE("`paypalSubscriptionId`");
    }
    public static WHERE wherePaypalCaptureId() {
        return new WHERE("`paypalCaptureId`");
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
        public List<Payment> get()  {
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                return Payment.get(sqlBuilder.toString()+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                return Payment.get(sqlBuilder.toString()+orderBy+limitBuilder.toString(), (Object[]) null);
        }

        /**
         * Executes the generated SQL statement
         * and removes the objects matching the query.
         */
        public void remove()  {
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                Payment.remove(sqlBuilder.toString()+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                Payment.remove(sqlBuilder.toString()+orderBy+limitBuilder.toString(), (Object[]) null);
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
     * @see PayHook#onPaymentAuthorized
     */
    public static List<Payment> getAuthorizedPayments() throws Exception {
        return getAuthorizedPayments(null);
    }

    /**
     * List of payments that have been authorized/completed/paid.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is not null.
     * @see PayHook#onPaymentAuthorized
     */
    public static List<Payment> getAuthorizedPayments(String where) throws Exception {
        return get("timestampAuthorized != 0" + (where != null ? " AND " + where : ""));
    }

    /**
     * List of payments that have been cancelled (or expired).
     *
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#onPaymentCancelled
     */
    public static List<Payment> getCancelledPayments() throws Exception {
        return getCancelledPayments(null);
    }

    /**
     * List of payments that have been cancelled (or expired).
     *
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#onPaymentCancelled
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

    public PaymentProcessor getPaymentProcessor() {
        if (isPayPalSupported()) return PaymentProcessor.PAYPAL;
        else if (isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
        // TODO ADD NEW PROCESSORS
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
        return interval != 0;
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
