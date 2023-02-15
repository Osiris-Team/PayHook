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
        assertEquals(0, Payment.getSubscriptionPaymentsForUser("test").size());
        p.paypalSubscriptionId = "not-null";
        Payment.update(p);
        assertEquals(1, Payment.getSubscriptionPaymentsForUser("test").size());
    }

    @Test
    void getPendingFuturePayments() {
    }

    @Test
    void getPendingPayments() {
    }

    @Test
    void getAuthorizedPayments() {
    }

    @Test
    void getCancelledPayments() {
    }

    @Test
    void getRefundedPayments() {
    }
}