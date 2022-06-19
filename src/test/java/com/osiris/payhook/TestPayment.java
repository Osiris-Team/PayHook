package com.osiris.payhook;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestPayment {
    private static final java.sql.Connection con;
    private static final java.util.concurrent.atomic.AtomicInteger idCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    static {
        try {
            con = java.sql.DriverManager.getConnection(Database.url, Database.username, Database.password);
            try (Statement s = con.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS `TestPayment` (id INT NOT NULL PRIMARY KEY)");
                s.executeUpdate("ALTER TABLE `TestPayment` ADD COLUMN IF NOT EXISTS id INT NOT NULL PRIMARY KEY");
                s.executeUpdate("ALTER TABLE `TestPayment` MODIFY IF EXISTS id INT NOT NULL PRIMARY KEY");
                s.executeUpdate("ALTER TABLE `TestPayment` ADD COLUMN IF NOT EXISTS name VARCHAR(255)");
                s.executeUpdate("ALTER TABLE `TestPayment` MODIFY IF EXISTS name VARCHAR(255)");
                s.executeUpdate("ALTER TABLE `TestPayment` ADD COLUMN IF NOT EXISTS age BIGINT");
                s.executeUpdate("ALTER TABLE `TestPayment` MODIFY IF EXISTS age BIGINT");
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM `TestPayment` ORDER BY id DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCounter.set(rs.getInt(1));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Database field/value. <br>
     */
    public int id;
    /**
     * Database field/value. <br>
     */
    public String name;
    /**
     * Database field/value. <br>
     */
    public long age;
    public TestPayment() {
    }

    /**
     * Increments the id and sets it for this object (basically reserves a space in the database).
     *
     * @return object with latest id. Should be added to the database next by you.
     */
    public static TestPayment create() {
        TestPayment obj = new TestPayment();
        obj.id = idCounter.incrementAndGet();
        return obj;
    }

    public static List<TestPayment> get() throws Exception {
        return get(null);
    }

    /**
     * @return a list containing only objects that match the provided SQL WHERE statement.
     * if that statement is null, returns all the contents of this table.
     */
    public static List<TestPayment> get(String where) throws Exception {
        List<TestPayment> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id,name,age" +
                        " FROM `TestPayment`" +
                        (where != null ? ("WHERE " + where) : ""))) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TestPayment obj = new TestPayment();
                list.add(obj);
                obj.id = rs.getInt(1);
                obj.name = rs.getString(2);
                obj.age = rs.getLong(3);
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
    public static void update(TestPayment obj) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE `TestPayment` SET id=?,name=?,age=?")) {
            ps.setInt(1, obj.id);
            ps.setString(2, obj.name);
            ps.setLong(3, obj.age);
            ps.executeUpdate();
        }
    }

    /**
     * Adds the provided object to the database (note that the id is not checked for duplicates).
     */
    public static void add(TestPayment obj) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO `TestPayment` (id,name,age) VALUES (?,?,?)")) {
            ps.setInt(1, obj.id);
            ps.setString(2, obj.name);
            ps.setLong(3, obj.age);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes the provided object from the database.
     */
    public static void delete(TestPayment obj) throws Exception {
        delete("id = " + obj.id);
    }

    /**
     * Deletes the objects that are found by the provided SQL WHERE statement, from the database.
     */
    public static void delete(String where) throws Exception {
        java.util.Objects.requireNonNull(where);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM `TestPayment` WHERE " + where)) {
            ps.executeUpdate();
        }
    }

}
