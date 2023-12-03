package com.osiris.jsqlgen.payhook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
Generated class by <a href="https://github.com/Osiris-Team/jSQL-Gen">jSQL-Gen</a>
that contains static methods for fetching/updating data from the "WebhookEndpoint" table.
A single object/instance of this class represents a single row in the table
and data can be accessed via its public fields. <p>
Its not recommended to modify this class but it should be OK to add new methods to it.
If modifications are really needed create a pull request directly to jSQL-Gen instead. <br>
NO EXCEPTIONS is enabled which makes it possible to use this methods outside of try/catch blocks because SQL errors will be caught and thrown as runtime exceptions instead. <br>
*/
public class WebhookEndpoint{
public static java.util.concurrent.atomic.AtomicInteger idCounter = new java.util.concurrent.atomic.AtomicInteger(0);
static {
try{
Connection con = Database.getCon();
try{
try (Statement s = con.createStatement()) {
s.executeUpdate("CREATE TABLE IF NOT EXISTS `webhookendpoint` (`id` INT NOT NULL PRIMARY KEY)");
try{s.executeUpdate("ALTER TABLE `webhookendpoint` ADD COLUMN `url` TEXT NOT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `webhookendpoint` MODIFY COLUMN `url` TEXT NOT NULL");
try{s.executeUpdate("ALTER TABLE `webhookendpoint` ADD COLUMN `stripeWebhookSecret` TEXT NOT NULL");}catch(Exception ignored){}
s.executeUpdate("ALTER TABLE `webhookendpoint` MODIFY COLUMN `stripeWebhookSecret` TEXT NOT NULL");
}
try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `webhookendpoint` ORDER BY id DESC LIMIT 1")) {
ResultSet rs = ps.executeQuery();
if (rs.next()) idCounter.set(rs.getInt(1) + 1);
}
}
catch(Exception e){ throw new RuntimeException(e); }
finally {Database.freeCon(con);}
}catch(Exception e){
e.printStackTrace();
System.err.println("Something went really wrong during table (WebhookEndpoint) initialisation, thus the program will exit!");System.exit(1);}
}

/**
Use the static create method instead of this constructor,
if you plan to add this object to the database in the future, since
that method fetches and sets/reserves the {@link #id}.
*/
public WebhookEndpoint (int id, String url, String stripeWebhookSecret){
this.id = id;this.url = url;this.stripeWebhookSecret = stripeWebhookSecret;
}
/**
Database field/value. Not null. <br>
*/
public int id;
/**
Database field/value. Not null. <br>
*/
public String url;
/**
Database field/value. Not null. <br>
*/
public String stripeWebhookSecret;
/**
Creates and returns an object that can be added to this table.
Increments the id (thread-safe) and sets it for this object (basically reserves a space in the database).
Note that the parameters of this method represent "NOT NULL" fields in the table and thus should not be null.
Also note that this method will NOT add the object to the table.
*/
public static WebhookEndpoint create( String url, String stripeWebhookSecret) {
int id = idCounter.getAndIncrement();
WebhookEndpoint obj = new WebhookEndpoint(id, url, stripeWebhookSecret);
return obj;
}

/**
Convenience method for creating and directly adding a new object to the table.
Note that the parameters of this method represent "NOT NULL" fields in the table and thus should not be null.
*/
public static WebhookEndpoint createAndAdd( String url, String stripeWebhookSecret)  {
int id = idCounter.getAndIncrement();
WebhookEndpoint obj = new WebhookEndpoint(id, url, stripeWebhookSecret);
add(obj);
return obj;
}

/**
@return a list containing all objects in this table.
*/
public static List<WebhookEndpoint> get()  {return get(null);}
/**
@return object with the provided id or null if there is no object with the provided id in this table.
@throws Exception on SQL issues.
*/
public static WebhookEndpoint get(int id)  {
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
public static List<WebhookEndpoint> get(String where, Object... whereValues)  {
String sql = "SELECT `id`,`url`,`stripeWebhookSecret`" +
" FROM `webhookendpoint`" +
(where != null ? where : "");
List<WebhookEndpoint> list = new ArrayList<>();
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(sql)) {
if(where!=null && whereValues!=null)
for (int i = 0; i < whereValues.length; i++) {
Object val = whereValues[i];
ps.setObject(i+1, val);
}
ResultSet rs = ps.executeQuery();
while (rs.next()) {
WebhookEndpoint obj = new WebhookEndpoint();
list.add(obj);
obj.id = rs.getInt(1);
obj.url = rs.getString(2);
obj.stripeWebhookSecret = rs.getString(3);
}
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
return list;
}

    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<WebhookEndpoint>> onResultReceived){
        getLazy(onResultReceived, null, 500, null);
    }
    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<WebhookEndpoint>> onResultReceived, int limit){
        getLazy(onResultReceived, null, limit, null);
    }
    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<WebhookEndpoint>> onResultReceived, Consumer<Long> onFinish){
        getLazy(onResultReceived, onFinish, 500, null);
    }
    /**
     * See {@link #getLazy(Consumer, Consumer, int, WHERE)} for details.
     */
    public static void getLazy(Consumer<List<WebhookEndpoint>> onResultReceived, Consumer<Long> onFinish, int limit){
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
    public static void getLazy(Consumer<List<WebhookEndpoint>> onResultReceived, Consumer<Long> onFinish, int limit, WHERE where) {
        new Thread(() -> {
            WHERE finalWhere;
            if(where == null) finalWhere = new WHERE("");
            else finalWhere = where;
            List<WebhookEndpoint> results;
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

public static int count(){ return count(null, null); }

public static int count(String where, Object... whereValues)  {
String sql = "SELECT COUNT(`id`) AS recordCount FROM `webhookendpoint`" +
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
public static void update(WebhookEndpoint obj)  {
String sql = "UPDATE `webhookendpoint` SET `id`=?,`url`=?,`stripeWebhookSecret`=? WHERE id="+obj.id;
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(sql)) {
ps.setInt(1, obj.id);
ps.setString(2, obj.url);
ps.setString(3, obj.stripeWebhookSecret);
ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
}

/**
Adds the provided object to the database (note that the id is not checked for duplicates).
*/
public static void add(WebhookEndpoint obj)  {
String sql = "INSERT INTO `webhookendpoint` (`id`,`url`,`stripeWebhookSecret`) VALUES (?,?,?)";
Connection con = Database.getCon();
try (PreparedStatement ps = con.prepareStatement(sql)) {
ps.setInt(1, obj.id);
ps.setString(2, obj.url);
ps.setString(3, obj.stripeWebhookSecret);
ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
finally{Database.freeCon(con);}
}

/**
Deletes the provided object from the database.
*/
public static void remove(WebhookEndpoint obj)  {
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
String sql = "DELETE FROM `webhookendpoint` "+where;
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
String sql = "DELETE FROM `webhookendpoint`";
Connection con = Database.getCon();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
}catch(Exception e){throw new RuntimeException(e);}
        finally{Database.freeCon(con);}
    }

public WebhookEndpoint clone(){
return new WebhookEndpoint(this.id,this.url,this.stripeWebhookSecret);
}
public String toPrintString(){
return  ""+"id="+this.id+" "+"url="+this.url+" "+"stripeWebhookSecret="+this.stripeWebhookSecret+" ";
}
public static WHERE<Integer> whereId() {
return new WHERE<Integer>("`id`");
}
public static WHERE<String> whereUrl() {
return new WHERE<String>("`url`");
}
public static WHERE<String> whereStripeWebhookSecret() {
return new WHERE<String>("`stripeWebhookSecret`");
}
public static class WHERE<T> {
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
        public List<WebhookEndpoint> get()  {
            String where = sqlBuilder.toString();
            if(!where.isEmpty()) where = " WHERE " + where;
            String orderBy = orderByBuilder.toString();
            if(!orderBy.isEmpty()) orderBy = " ORDER BY "+orderBy.substring(0, orderBy.length()-2)+" ";
            if(!whereObjects.isEmpty())
                return WebhookEndpoint.get(where+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                return WebhookEndpoint.get(where+orderBy+limitBuilder.toString(), (T[]) null);
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
                return WebhookEndpoint.count(where+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                return WebhookEndpoint.count(where+orderBy+limitBuilder.toString(), (T[]) null);
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
                WebhookEndpoint.remove(where+orderBy+limitBuilder.toString(), whereObjects.toArray());
            else
                WebhookEndpoint.remove(where+orderBy+limitBuilder.toString(), (T[]) null);
        }

        /**
         * AND (...) <br>
         */
        public WHERE<T> and(WHERE<?> where) {
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
        public WHERE<T> or(WHERE<?> where) {
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
        public WHERE<T> is(T obj) {
            sqlBuilder.append(columnName).append(" = ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName IN (?,?,...) <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_in.asp">https://www.w3schools.com/mysql/mysql_in.asp</a>
         */
        public WHERE<T> is(T... objects) {
            String s = "";
            for (T obj : objects) {
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
        public WHERE<T> isNot(T obj) {
            sqlBuilder.append(columnName).append(" <> ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName IS NULL <br>
         */
        public WHERE<T> isNull() {
            sqlBuilder.append(columnName).append(" IS NULL ");
            return this;
        }

        /**
         * columnName IS NOT NULL <br>
         */
        public WHERE<T> isNotNull() {
            sqlBuilder.append(columnName).append(" IS NOT NULL ");
            return this;
        }

        /**
         * columnName LIKE ? <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE<T> like(T obj) {
            sqlBuilder.append(columnName).append(" LIKE ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName NOT LIKE ? <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE<T> notLike(T obj) {
            sqlBuilder.append(columnName).append(" NOT LIKE ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName > ? <br>
         */
        public WHERE<T> biggerThan(T obj) {
            sqlBuilder.append(columnName).append(" > ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName < ? <br>
         */
        public WHERE<T> smallerThan(T obj) {
            sqlBuilder.append(columnName).append(" < ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName >= ? <br>
         */
        public WHERE<T> biggerOrEqual(T obj) {
            sqlBuilder.append(columnName).append(" >= ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName <= ? <br>
         */
        public WHERE<T> smallerOrEqual(T obj) {
            sqlBuilder.append(columnName).append(" <= ? ");
            whereObjects.add(obj);
            return this;
        }

        /**
         * columnName BETWEEN ? AND ? <br>
         */
        public WHERE<T> between(T obj1, T obj2) {
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
        public WHERE<T> smallestFirst() {
            orderByBuilder.append(columnName + " ASC, ");
            return this;
        }

        /**
         * columnName DESC, <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_like.asp">https://www.w3schools.com/mysql/mysql_like.asp</a>
         */
        public WHERE<T> biggestFirst() {
            orderByBuilder.append(columnName + " DESC, ");
            return this;
        }

        /**
         * LIMIT number <br>
         *
         * @see <a href="https://www.w3schools.com/mysql/mysql_limit.asp">https://www.w3schools.com/mysql/mysql_limit.asp</a>
         */
        public WHERE<T> limit(int num) {
            limitBuilder.append("LIMIT ").append(num + " ");
            return this;
        }

    }
// The code below will not be removed when re-generating this class.
// Additional code start -> 
private WebhookEndpoint(){}
// Additional code end <- 
}
