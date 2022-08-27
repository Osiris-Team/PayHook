package com.osiris.payhook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class Database {
    // TODO: Insert credentials and update url.
    public static String rawUrl = "jdbc:mysql://localhost/";
    public static String url = "jdbc:mysql://localhost/test";
    public static String name = "test";
    public static String username = "root";
    public static String password = "";

    public static void create() {

        // Do the below to avoid "No suitable driver found..." exception
        String driverClassName = "com.mysql.cj.jdbc.Driver";
        try {
            Class<?> driverClass = Class.forName(driverClassName);
            Objects.requireNonNull(driverClass);
        } catch (ClassNotFoundException e) {
            try {
                driverClassName = "com.mysql.jdbc.Driver"; // Try deprecated driver as fallback
                Class<?> driverClass = Class.forName(driverClassName);
                Objects.requireNonNull(driverClass);
            } catch (ClassNotFoundException ex) {
                System.err.println("Failed to find critical database driver class: " + driverClassName);
                ex.printStackTrace();
            }
        }

        // Create database if not exists
        try (Connection c = DriverManager.getConnection(Database.rawUrl, Database.username, Database.password);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + Database.name + "`");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

