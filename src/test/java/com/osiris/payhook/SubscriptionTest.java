package com.osiris.payhook;

import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.payhook.utils.UtilsTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionTest {
    @Test
    void test() {
        long now = System.currentTimeMillis();
        int interval = Payment.Interval.MONTHLY;
        long timestampCreated = now;
        long timestampExpires = now + 1000;
        long timestampAuthorized = now;
        long timestampCancelled = 0;
        long timestampRefunded = 0;
        Payment payment = new Payment(0, "user", 100, "EUR", interval, "", 0, "my-sub",
                1, timestampCreated, timestampExpires, timestampAuthorized, timestampCancelled, timestampRefunded, null, "stripe-id", null, null, null, null);
        List<Payment> payments = new ArrayList<>();
        payments.add(payment);
        Subscription sub = new Subscription(payments);
        assertTrue(sub.getLastPayment() == payment);
        assertTrue(sub.getMillisLeft() >= UtilsTime.MS_MONTH - UtilsTime.MS_MINUTE);
        assertTrue(sub.getMillisLeftWithPuffer() > sub.getMillisLeft() && sub.getMillisLeftWithPuffer() > UtilsTime.MS_MONTH);
        assertTrue(sub.isTimeLeft());
        assertFalse(sub.isCancelled());
    }
}