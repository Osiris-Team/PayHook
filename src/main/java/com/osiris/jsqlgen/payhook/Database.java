package com.osiris.jsqlgen.payhook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
Auto-generated class that is used by all table classes to create connections. <br>
It holds the database credentials (set by you at first run of jSQL-Gen).<br>
Note that the fields rawUrl, url, username and password do NOT get overwritten when re-generating this class. <br>
All tables use the cached connection pool in this class which has following advantages: <br>
- Ensures optimal performance (cpu and memory usage) for any type of database from small to huge, with millions of queries per second.
- Connection status is checked before doing a query (since it could be closed or timed out and thus result in errors).*/
public class Database{
public static String rawUrl = com.osiris.payhook.PayHook.databaseRawUrl;
public static String url = com.osiris.payhook.PayHook.databaseUrl;
public static String name = com.osiris.payhook.PayHook.databaseName;
public static String username = com.osiris.payhook.PayHook.databaseUsername;
public static String password = com.osiris.payhook.PayHook.databasePassword;
private static final List<Connection> availableConnections = new ArrayList<>();

    static{create();} // Create database if not exists

public static void create() {

        // Do the below to avoid "No suitable driver found..." exception
        String[] driversClassNames = new String[]{"com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver",
        "oracle.jdbc.OracleDriver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "org.postgresql.Driver",
        "org.sqlite.JDBC", "org.h2.Driver", "com.ibm.db2.jcc.DB2Driver", "org.apache.derby.jdbc.ClientDriver",
        "org.mariadb.jdbc.Driver", "org.apache.derby.jdbc.ClientDriver"};
        Class<?> driverClass = null;
        Exception lastException = null;
    for (int i = 0; i < driversClassNames.length; i++) {
        String driverClassName = driversClassNames[i];
        try {
            driverClass = Class.forName(driverClassName);
            Objects.requireNonNull(driverClass);
        } catch (Exception e) {
            lastException = e;
        }
    }
    if(driverClass == null){
        if(lastException != null) lastException.printStackTrace();
        System.err.println("Failed to find critical database driver class, program will exit! Searched classes: "+ Arrays.toString(driversClassNames));
        System.exit(1);
    }

        // Create database if not exists
        try(Connection c = DriverManager.getConnection(Database.rawUrl, Database.username, Database.password);
            Statement s = c.createStatement();) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `"+Database.name+"`");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Something went really wrong during database initialisation, program will exit.");
            System.exit(1);
        }
    }

    public static Connection getCon() {
        synchronized (availableConnections){
            try{
                if (!availableConnections.isEmpty()) {
                    List<Connection> removableConnections = new ArrayList<>(0);
                    for (Connection con : availableConnections) {
                        if (con.isValid(1)) return con;
                        else removableConnections.add(con);
                    }
                    for (Connection removableConnection : removableConnections) {
                        removableConnection.close();
                        availableConnections.remove(removableConnection); // Remove invalid connections
                    }
                }
                return DriverManager.getConnection(Database.url, Database.username, Database.password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void freeCon(Connection connection) {
        synchronized (availableConnections){
            availableConnections.add(connection);
        }
    }
}
