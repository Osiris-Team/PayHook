package com.osiris.payhook;

import ch.vorburger.exec.ManagedProcessException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Create test-credentials.txt inside the project root directory. <br>
 * First line is the stripe secret key. <br>
 */
public class TestUtils {
    public static SQLTestServer server;
    public static String dbUrl;
    public static String dbUsername = "root";
    public static String dbPassword = "";
    public static String stripeSecretKey = null;

    static{
        try {
            server = SQLTestServer.buildAndRun("testDB");
            dbUrl = server.getUrl();
        } catch (ManagedProcessException e) {
            e.printStackTrace();
        }
    }

    public static void fetchCrendentials() throws Exception {
        if(stripeSecretKey!=null) return;
        File f = new File(System.getProperty("user.dir")+"/test-credentials.txt");
        if (!f.exists()) throw new FileNotFoundException("Make sure that the credentials file exists at: "+f.getAbsolutePath());
        try(BufferedReader reader = new BufferedReader(new FileReader(f))){
            stripeSecretKey = reader.readLine();
        }
        if(stripeSecretKey==null) throw new Exception("First line of test-credentials.txt must be the stripe secret key!");
    }

}
