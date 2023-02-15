package com.osiris.jsqlgen.payhook;

import ch.vorburger.exec.ManagedProcessException;
import com.osiris.payhook.DatabaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentTest {

    private static long now = System.currentTimeMillis();

    @Test
    void getSubscriptionPaymentsForUser() throws ManagedProcessException {
        DatabaseTest.init();
        Payment.whereUserId().is("test").remove();
        Payment.createAndAdd("test", 100, "EUR", Payment.Interval.NONE);
        Payment p = Payment.createAndAdd("test", 100, "EUR", Payment.Interval.MONTHLY);
        assertEquals(0, Payment.getUserSubscriptionPayments("test").size());
        p.paypalSubscriptionId = "not-null";
        Payment.update(p);
        assertEquals(1, Payment.getUserSubscriptionPayments("test").size());
    }

    @Test
    void getPendingPayments() throws ManagedProcessException {
        DatabaseTest.init();
        Payment.whereUserId().is("test").remove();
        Payment p = Payment.createAndAdd("test", 100, "EUR", Payment.Interval.NONE);
        p.timestampCreated = now - 1000;
        Payment.update(p);
        assertEquals(1, Payment.getUserPendingPayments("test").size());
    }

    @Test
    void getAuthorizedPayments() throws ManagedProcessException {
        DatabaseTest.init();
        Payment.whereUserId().is("test").remove();
        Payment p = Payment.createAndAdd("test", 100, "EUR", Payment.Interval.NONE);
        p.timestampAuthorized = now - 1000;
        Payment.update(p);
        assertEquals(1, Payment.getUserAuthorizedPayments("test").size());
    }

    @Test
    void getCancelledPayments() throws ManagedProcessException {
        DatabaseTest.init();
        Payment.whereUserId().is("test").remove();
        Payment p = Payment.createAndAdd("test", 100, "EUR", Payment.Interval.NONE);
        p.timestampCancelled = now - 1000;
        Payment.update(p);
        assertEquals(1, Payment.getUserCancelledPayments("test").size());
    }

    @Test
    void getRefundedPayments() throws ManagedProcessException {
        DatabaseTest.init();
        Payment.whereUserId().is("test").remove();
        Payment p = Payment.createAndAdd("test", 100, "EUR", Payment.Interval.NONE);
        p.timestampRefunded = now - 1000;
        Payment.update(p);
        assertEquals(1, Payment.getUserRefundedPayments("test").size());
    }
}