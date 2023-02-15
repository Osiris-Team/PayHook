package com.osiris.payhook;

import ch.vorburger.exec.ManagedProcessException;
import com.osiris.jsqlgen.payhook.Database;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseTest {
    private static boolean isInit = false;
    public static synchronized void init() throws ManagedProcessException {
        if(isInit) return;
        System.out.println("Starting database...");
        SQLTestServer sqlTestServer = SQLTestServer.buildAndRun();
        Database.url = sqlTestServer.getUrl();
        Database.username = "root";
        Database.password = "";
        System.out.println("Url: " + Database.url);
        System.out.println("OK!");
        isInit = true;
    }
    @Test
    void test() throws Exception {
        init();

        for (TestPayment p : TestPayment.get()) {
            TestPayment.delete(p);
        }
        assertTrue(TestPayment.get().isEmpty());

        TestPayment testPayment = TestPayment.create();
        testPayment.age = 1;
        testPayment.name = "My first payment.";
        TestPayment.add(testPayment);

        for (TestPayment p : TestPayment.get()) {
            System.out.println(p.id + " " + p.age + " " + p.name);
        }
        assertEquals(1, TestPayment.get().size());
        assertEquals(1, TestPayment.get("age = 1").size());
        assertEquals(0, TestPayment.get("age = 0").size());

        testPayment.name = "New payment name!";
        TestPayment.update(testPayment);
        assertEquals(testPayment.name, TestPayment.get().get(0).name);

        TestPayment.delete(testPayment);
        assertTrue(TestPayment.get().isEmpty());
    }
}
