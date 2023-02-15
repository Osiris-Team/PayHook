package com.osiris.jsqlgen.payhook;

import com.osiris.payhook.PayHook;
import com.osiris.payhook.PaymentProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
Generated class by <a href="https://github.com/Osiris-Team/jSQL-Gen">jSQL-Gen</a>
that contains static methods for fetching/updating data from the "Payment" table.
A single object/instance of this class represents a single row in the table
and data can be accessed via its public fields. <p>
Its not recommended to modify this class but it should be OK to add new methods to it.
If modifications are really needed create a pull request directly to jSQL-Gen instead. <br>
NO EXCEPTIONS is enabled which makes it possible to use this methods outside of try/catch blocks because SQL errors will be caught and thrown as runtime exceptions instead. <br>
*/
public class Payment{
private static java.util.concurrent.atomic.AtomicInteger idCounter = new java.util.concurrent.atomic.AtomicInteger(0);
static {
try{
Connection con = Database.getCon();
try{
try (Statement s = con.createStatement()) {
s.executeUpdate("CREATE TABLE IF NOT EXISTS `payment` (`id` INT NOT NULL PRIMARY KEY)");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `userId` TEXT(65532) NOT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `userId` TEXT(65532) NOT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `charge` BIGINT NOT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `charge` BIGINT NOT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `currency` CHAR(3) NOT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `currency` CHAR(3) NOT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `interval` INT NOT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `interval` INT NOT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `url` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `url` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `productId` INT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `productId` INT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `productName` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `productName` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `productQuantity` INT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `productQuantity` INT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `timestampCreated` BIGINT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `timestampCreated` BIGINT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `timestampExpires` BIGINT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `timestampExpires` BIGINT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `timestampAuthorized` BIGINT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `timestampAuthorized` BIGINT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `timestampCancelled` BIGINT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `timestampCancelled` BIGINT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `timestampRefunded` BIGINT DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `timestampRefunded` BIGINT DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `stripeSessionId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `stripeSessionId` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `stripeSubscriptionId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `stripeSubscriptionId` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `stripePaymentIntentId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `stripePaymentIntentId` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `paypalOrderId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `paypalOrderId` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `paypalSubscriptionId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `paypalSubscriptionId` TEXT(65532) DEFAULT NULL");
try{s.executeUpdate("ALTER TABLE `payment` ADD COLUMN `paypalCaptureId` TEXT(65532) DEFAULT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `payment` MODIFY COLUMN `paypalCaptureId` TEXT(65532) DEFAULT NULL");
}
try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `payment` ORDER BY id DESC LIMIT 1")) {
ResultSet rs = ps.executeQuery();
if (rs.next()) idCounter.set(rs.getInt(1) + 1);
}
}
catch(Exception e){ throw new RuntimeException(e); }
finally {Database.freeCon(con);}
}catch(Exception e){
e.printStackTrace();
System.err.println("Something went really wrong during table (Payment) initialisation, thus the program will exit!");System.exit(1);}
}

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
public Payment (int id, String userId, long charge, String currency, int interval, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, long timestampRefunded, String stripeSessionId, String stripeSubscriptionId, String stripePaymentIntentId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId){
this.id = id;this.userId = userId;this.charge = charge;this.currency = currency;this.interval = interval;this.url = url;this.productId = productId;this.productName = productName;this.productQuantity = productQuantity;this.timestampCreated = timestampCreated;this.timestampExpires = timestampExpires;this.timestampAuthorized = timestampAuthorized;this.timestampCancelled = timestampCancelled;this.timestampRefunded = timestampRefunded;this.stripeSessionId = stripeSessionId;this.stripeSubscriptionId = stripeSubscriptionId;this.stripePaymentIntentId = stripePaymentIntentId;this.paypalOrderId = paypalOrderId;this.paypalSubscriptionId = paypalSubscriptionId;this.paypalCaptureId = paypalCaptureId;
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
The total charged amount in the smallest form of money. Example: 100 cents == 1EUR.If not authorized yet, the money was not yet received.When refunded this normally is 0, or something greater on a partial refund.Note that cancelled does not mean refunded.
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
public long timestampRefunded;
/**
Database field/value. <br>
*/
public String stripeSessionId;
/**
Database field/value. <br>
*/
public String stripeSubscriptionId;
/**
Database field/value. <br>
*/
public String stripePaymentIntentId;
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
public static Payment create( String userId, long charge, String currency, int interval, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, long timestampRefunded, String stripeSessionId, String stripeSubscriptionId, String stripePaymentIntentId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId)  {
int id = idCounter.getAndIncrement();
Payment obj = new Payment();
obj.id=id; obj.userId=userId; obj.charge=charge; obj.currency=currency; obj.interval=interval; obj.url=url; obj.productId=productId; obj.productName=productName; obj.productQuantity=productQuantity; obj.timestampCreated=timestampCreated; obj.timestampExpires=timestampExpires; obj.timestampAuthorized=timestampAuthorized; obj.timestampCancelled=timestampCancelled; obj.timestampRefunded=timestampRefunded; obj.stripeSessionId=stripeSessionId; obj.stripeSubscriptionId=stripeSubscriptionId; obj.stripePaymentIntentId=stripePaymentIntentId; obj.paypalOrderId=paypalOrderId; obj.paypalSubscriptionId=paypalSubscriptionId; obj.paypalCaptureId=paypalCaptureId; 
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
public static Payment createAndAdd( String userId, long charge, String currency, int interval, String url, int productId, String productName, int productQuantity, long timestampCreated, long timestampExpires, long timestampAuthorized, long timestampCancelled, long timestampRefunded, String stripeSessionId, String stripeSubscriptionId, String stripePaymentIntentId, String paypalOrderId, String paypalSubscriptionId, String paypalCaptureId)  {
int id = idCounter.getAndIncrement();
Payment obj = new Payment();
obj.id=id; obj.userId=userId; obj.charge=charge; obj.currency=currency; obj.interval=interval; obj.url=url; obj.productId=productId; obj.productName=productName; obj.productQuantity=productQuantity; obj.timestampCreated=timestampCreated; obj.timestampExpires=timestampExpires; obj.timestampAuthorized=timestampAuthorized; obj.timestampCancelled=timestampCancelled; obj.timestampRefunded=timestampRefunded; obj.stripeSessionId=stripeSessionId; obj.stripeSubscriptionId=stripeSubscriptionId; obj.stripePaymentIntentId=stripePaymentIntentId; obj.paypalOrderId=paypalOrderId; obj.paypalSubscriptionId=paypalSubscriptionId; obj.paypalCaptureId=paypalCaptureId; 
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
return get("WHERE id = "+id).get(0);
}catch(IndexOutOfBoundsException ignored){}
catch(Exception e){throw new RuntimeException(e);}
return null;
}
/**
Example: <br>
get("WHERE username=? AND age=?", "Peter", 33);  <br>
@param where can be null. Your SQL WHERE statement (with the leading WHERE).
@param whereValues can be null. Your SQL WHERE statement values to set for '?'.
@return a list containing only objects that match the provided SQL WHERE statement (no matches = empty list).
if that statement is null, returns all the contents of this table.
*/
public static List<Payment> get(String where, Object... whereValues)  {
Connection con = Database.getCon();
String sql = "SELECT `id`,`userId`,`charge`,`currency`,`interval`,`url`,`productId`,`productName`,`productQuantity`,`timestampCreated`,`timestampExpires`,`timestampAuthorized`,`timestampCancelled`,`timestampRefunded`,`stripeSessionId`,`stripeSubscriptionId`,`stripePaymentIntentId`,`paypalOrderId`,`paypalSubscriptionId`,`paypalCaptureId`" +
" FROM `payment`" +
(where != null ? where : "");
List<Payment> list = new ArrayList<>();
try (PreparedStatement ps = con.prepareStatement(sql)) {
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
obj.timestampRefunded = rs.getLong(14);
obj.stripeSessionId = rs.getString(15);
obj.stripeSubscriptionId = rs.getString(16);
obj.stripePaymentIntentId = rs.getString(17);
obj.paypalOrderId = rs.getString(18);
obj.paypalSubscriptionId = rs.getString(19);
obj.paypalCaptureId = rs.getString(20);
}
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
return list;
}

    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<Payment>> onResultReceived){
        getLazy(onResultReceived, null, 500, null);
    }
    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<Payment>> onResultReceived, int limit){
        getLazy(onResultReceived, null, limit, null);
    }
    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<Payment>> onResultReceived, Consumer<Long> onFinish){
        getLazy(onResultReceived, onFinish, 500, null);
    }
    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<Payment>> onResultReceived, Consumer<Long> onFinish, int limit){
        getLazy(onResultReceived, onFinish, limit, null);
    }
    /**
     * Loads results lazily in a new thread. <br>
     * Add {@link Thread#sleep(long)} at the end of your onResultReceived code, to sleep between fetches.
     * @param onResultReceived can NOT be null. Gets executed until there are no results left, thus the results list is never empty.
     * @param onFinish can be null. Gets executed when finished receiving all results. Provides the total amount of received elements as parameter.
     * @param limit the maximum amount of elements for each fetch.
     * @param where can be null. This WHERE is not allowed to contain LIMIT and should not contain order by id.
     */
    public static void getLazy(Consumer<List<Payment>> onResultReceived, Consumer<Long> onFinish, int limit, WHERE where) {
        new Thread(() -> {
            WHERE finalWhere;
            if(where == null) finalWhere = new WHERE("");
            else finalWhere = where;
            List<Payment> results;
            int lastId = -1;
            long count = 0;
            while(true){
                results = whereId().biggerThan(lastId).and(finalWhere).limit(limit).get();
                if(results.isEmpty()) break;
                lastId = results.get(results.size() - 1).id;
                count += results.size();
                onResultReceived.accept(results);
            }
            if(onFinish!=null) onFinish.accept(count);
        }).start();
    }

public static int count(String where, Object... whereValues)  {
String sql = "SELECT COUNT(`id`) AS recordCount FROM `payment`" +
(where != null ? where : ""); 
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(sql)) {
if(where!=null && whereValues!=null)
for (int i = 0; i < whereValues.length; i++) {
Object val = whereValues[i];
ps.setObject(i+1, val);
}
ResultSet rs = ps.executeQuery();
if (rs.next()) return rs.getInt("recordCount");
}catch(Exception e){throw new RuntimeException(e);}
finally {Database.freeCon(con);}
return 0;
}

/**
Searches the provided object in the database (by its id),
and updates all its fields.
@throws Exception when failed to find by id or other SQL issues.
*/
public static void update(Payment obj)  {
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(
                "UPDATE `payment` SET `id`=?,`userId`=?,`charge`=?,`currency`=?,`interval`=?,`url`=?,`productId`=?,`productName`=?,`productQuantity`=?,`timestampCreated`=?,`timestampExpires`=?,`timestampAuthorized`=?,`timestampCancelled`=?,`timestampRefunded`=?,`stripeSessionId`=?,`stripeSubscriptionId`=?,`stripePaymentIntentId`=?,`paypalOrderId`=?,`paypalSubscriptionId`=?,`paypalCaptureId`=? WHERE id="+obj.id)) {
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
ps.setLong(14, obj.timestampRefunded);
ps.setString(15, obj.stripeSessionId);
ps.setString(16, obj.stripeSubscriptionId);
ps.setString(17, obj.stripePaymentIntentId);
ps.setString(18, obj.paypalOrderId);
ps.setString(19, obj.paypalSubscriptionId);
ps.setString(20, obj.paypalCaptureId);
ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
}

/**
Adds the provided object to the database (note that the id is not checked for duplicates).
*/
public static void add(Payment obj)  {
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO `payment` (`id`,`userId`,`charge`,`currency`,`interval`,`url`,`productId`,`productName`,`productQuantity`,`timestampCreated`,`timestampExpires`,`timestampAuthorized`,`timestampCancelled`,`timestampRefunded`,`stripeSessionId`,`stripeSubscriptionId`,`stripePaymentIntentId`,`paypalOrderId`,`paypalSubscriptionId`,`paypalCaptureId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
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
ps.setLong(14, obj.timestampRefunded);
ps.setString(15, obj.stripeSessionId);
ps.setString(16, obj.stripeSubscriptionId);
ps.setString(17, obj.stripePaymentIntentId);
ps.setString(18, obj.paypalOrderId);
ps.setString(19, obj.paypalSubscriptionId);
ps.setString(20, obj.paypalCaptureId);
ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
}

/**
Deletes the provided object from the database.
*/
public static void remove(Payment obj)  {
remove("WHERE id = "+obj.id);
}
/**
Example: <br>
remove("WHERE username=?", "Peter"); <br>
Deletes the objects that are found by the provided SQL WHERE statement, from the database.
@param where can NOT be null.
@param whereValues can be null. Your SQL WHERE statement values to set for '?'.
*/
public static void remove(String where, Object... whereValues)  {
java.util.Objects.requireNonNull(where);
String sql = "DELETE FROM `payment` "+where;
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(sql)) {
if(whereValues != null)
                for (int i = 0; i < whereValues.length; i++) {
                    Object val = whereValues[i];
                    ps.setObject(i+1, val);
                }
ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
}

public static void removeAll()  {
        Connection con = Database.getCon();
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM `payment`")) {
            ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
        finally{Database.freeCon(con);}
    }

public Payment clone(){
return new Payment(this.id,this.userId,this.charge,this.currency,this.interval,this.url,this.productId,this.productName,this.productQuantity,this.timestampCreated,this.timestampExpires,this.timestampAuthorized,this.timestampCancelled,this.timestampRefunded,this.stripeSessionId,this.stripeSubscriptionId,this.stripePaymentIntentId,this.paypalOrderId,this.paypalSubscriptionId,this.paypalCaptureId);
}
public String toPrintString(){
return  ""+"id="+this.id+" "+"userId="+this.userId+" "+"charge="+this.charge+" "+"currency="+this.currency+" "+"interval="+this.interval+" "+"url="+this.url+" "+"productId="+this.productId+" "+"productName="+this.productName+" "+"productQuantity="+this.productQuantity+" "+"timestampCreated="+this.timestampCreated+" "+"timestampExpires="+this.timestampExpires+" "+"timestampAuthorized="+this.timestampAuthorized+" "+"timestampCancelled="+this.timestampCancelled+" "+"timestampRefunded="+this.timestampRefunded+" "+"stripeSessionId="+this.stripeSessionId+" "+"stripeSubscriptionId="+this.stripeSubscriptionId+" "+"stripePaymentIntentId="+this.stripePaymentIntentId+" "+"paypalOrderId="+this.paypalOrderId+" "+"paypalSubscriptionId="+this.paypalSubscriptionId+" "+"paypalCaptureId="+this.paypalCaptureId+" ";
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
public static WHERE whereTimestampRefunded() {
return new WHERE("`timestampRefunded`");
}
public static WHERE whereStripeSessionId() {
return new WHERE("`stripeSessionId`");
}
public static WHERE whereStripeSubscriptionId() {
return new WHERE("`stripeSubscriptionId`");
}
public static WHERE whereStripePaymentIntentId() {
return new WHERE("`stripePaymentIntentId`");
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
            String where = sqlBuilder.toString();
            if(!where.isEmpty()) where = " WHERE " + where;
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                return Payment.get(where+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                return Payment.get(where+orderBy+limitBuilder.toString(), (Object[]) null);
        }

        /**
         * Executes the generated SQL statement
         * and returns the size of the list of objects matching the query.
         */
        public int count()  {
            String where = sqlBuilder.toString();
            if(!where.isEmpty()) where = " WHERE " + where;
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                return Payment.count(where+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                return Payment.count(where+orderBy+limitBuilder.toString(), (Object[]) null);
        }

        /**
         * Executes the generated SQL statement
         * and removes the objects matching the query.
         */
        public void remove()  {
            String where = sqlBuilder.toString();
            if(!where.isEmpty()) where = " WHERE " + where;
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                Payment.remove(where+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                Payment.remove(where+orderBy+limitBuilder.toString(), (Object[]) null);
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
// The code below will not be removed when re-generating this class.
// Additional code start -> 
private Payment(){}

    public static List<Payment> getForUser(String userId){
        return get("userId=?", userId);
    }

    public static List<Payment> getUserSubscriptionPayments(String userId){
        return whereUserId().is(userId)
                .and(wherePaypalSubscriptionId().isNotNull()
                        .or(whereStripeSubscriptionId().isNotNull()))
                .get();
        // TODO ADD NEW PAYMENT PROCESSOR
    }

    public static List<Payment> getUserPendingPayments(String userId){
        return getPendingPayments("userId=?", userId);
    }

    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is smaller than now and {@link Payment#timestampExpires} is bigger than now.
     */
    public static List<Payment> getPendingPayments() {
        return getPendingPayments(null);
    }

    /**
     * List of payments that haven't been authorized or cancelled (or expired) yet.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is null, and
     * {@link Payment#timestampCancelled} is null, and {@link Payment#timestampCreated} is smaller than now and {@link Payment#timestampExpires} is bigger than now.
     */
    public static List<Payment> getPendingPayments(String where, Object... objs) {
        return get("WHERE timestampAuthorized = 0 AND timestampCancelled = 0 "
                 + (where != null ? " AND " + where : ""), objs);
    }

    public static List<Payment> getUserAuthorizedPayments(String userId) {
        return getAuthorizedPayments("userId=?", userId);
    }

    /**
     * List of payments that have been authorized/completed/paid.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is not null.
     * @see PayHook#onPaymentAuthorized
     */
    public static List<Payment> getAuthorizedPayments() {
        return getAuthorizedPayments(null);
    }

    /**
     * List of payments that have been authorized/completed/paid.
     *
     * @return list of payments, where {@link Payment#timestampAuthorized} is not null.
     * @see PayHook#onPaymentAuthorized
     */
    public static List<Payment> getAuthorizedPayments(String where, Object... objs) {
        return get("WHERE timestampAuthorized != 0 " + (where != null ? " AND " + where : ""), objs);
    }

    public static List<Payment> getUserCancelledPayments(String userId) {
        return getCancelledPayments("userId=?", userId);
    }

    /**
     * List of payments that have been cancelled (or expired).
     *
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#onPaymentCancelled
     */
    public static List<Payment> getCancelledPayments() {
        return getCancelledPayments(null);
    }

    /**
     * List of payments that have been cancelled (or expired).
     *
     * @return list of payments, where {@link Payment#timestampCancelled} is not null.
     * @see PayHook#onPaymentCancelled
     */
    public static List<Payment> getCancelledPayments(String where, Object... objs) {
        return get("WHERE timestampCancelled != 0 " + (where != null ? " AND " + where : ""), objs);
    }

    public static List<Payment> getUserRefundedPayments(String userId) {
        return getRefundedPayments("userId=?", userId);
    }

    /**
     * List of payments that have been refunded.
     *
     * @return list of payments, where {@link Payment#timestampRefunded} is not 0.
     */
    public static List<Payment> getRefundedPayments() {
        return getRefundedPayments(null);
    }

    /**
     * List of payments that have been refunded.
     *
     * @return list of payments, where {@link Payment#timestampRefunded} is not 0.
     */
    public static List<Payment> getRefundedPayments(String where, Object... objs) {
        return get("WHERE timestampRefunded != 0 " + (where != null ? " AND " + where : ""), objs);
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
        return stripeSessionId != null || stripeSubscriptionId != null;
    }

    public long getUrlTimeoutMs() {
        PaymentProcessor paymentProcessor = getPaymentProcessor();
        if (paymentProcessor == PaymentProcessor.PAYPAL) return PayHook.paypalUrlTimeoutMs;
        else if (paymentProcessor == PaymentProcessor.STRIPE) return PayHook.stripeUrlTimeoutMs;
        else throw new IllegalArgumentException("Unknown/Invalid payment processor: " + paymentProcessor);
        // TODO ADD NEW PROCESSORS
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
        return timestampRefunded != 0;
    }

    public boolean isFree() {
        return charge == 0;
    }

    public boolean isFullyRefunded(){
        return isRefund() && isFree();
    }

    /**
     * Helper class to set the payments billing interval in days.
     */
    public static class Interval {
        /**
         * One time payment.
         */
        public static final int NONE = 0;
        /**
         * Recurring payment every month (exactly 30 days).
         */
        public static final int MONTHLY = 30;
        /**
         * Recurring payment every 3 months (exactly 90 days).
         */
        public static final int TRI_MONTHLY = 90;
        /**
         * Recurring payment every 6 months (exactly 180 days).
         */
        public static final int HALF_YEARLY = 180;
        /**
         * Recurring payment every 12 months (exactly 360 days).
         */
        public static final int YEARLY = 360;

        public static long toHours(long days) {
            return days * 24;
        }

        public static long toMilliseconds(long days) {
            return toHours(days) * 3600000;
        }
    }
// Additional code end <- 
}
