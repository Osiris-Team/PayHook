package com.osiris.payhook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Subscription {
    public List<Payment> payments;

    public Subscription(List<Payment> payments) {
        this.payments = payments;
    }

    public static List<Subscription> getForUser(String userId){
        //TODO ADD PAYMENT PROCESSOR
        List<Payment> payments = Payment.get("(paypalSubscriptionId IS NOT NULL OR stripeSubscriptionId IS NOT NULL)" +
                " AND userId=?", userId);
        return paymentsToSubscriptions(payments);
    }

    public static List<Subscription> get(){
        //TODO ADD PAYMENT PROCESSOR
        List<Payment> payments = Payment.get("paypalSubscriptionId IS NOT NULL OR stripeSubscriptionId IS NOT NULL");
        return paymentsToSubscriptions(payments);
    }

    public static List<Subscription> getActive(){
        //TODO ADD PAYMENT PROCESSOR
        List<Payment> payments = Payment.get("paypalSubscriptionId IS NOT NULL OR stripeSubscriptionId IS NOT NULL");
        List<Subscription> subs = paymentsToSubscriptions(payments);
        for (Subscription sub : subs) {
            if(sub.isCancelled())
                subs.remove(sub);
            else if(sub.getMillisLeft() < 1)
                subs.remove(sub);
        }
        return subs;
    }

    public static List<Subscription> paymentsToSubscriptions(List<Payment> payments){
        //TODO ADD PAYMENT PROCESSOR
        Map<String, List<Payment>> paypalSubs = new HashMap<>();
        Map<String, List<Payment>> stripeSubs = new HashMap<>();
        for (Payment payment : payments) {
            if(payment.paypalSubscriptionId != null){
                List<Payment> list = paypalSubs.computeIfAbsent(payment.paypalSubscriptionId, k -> new ArrayList<>());
                list.add(payment);
            }
        }
        for (Payment payment : payments) {
            if(payment.stripeSubscriptionId != null){
                List<Payment> list = stripeSubs.computeIfAbsent(payment.stripeSubscriptionId, k -> new ArrayList<>());
                list.add(payment);
            }
        }

        List<Subscription> subs = new ArrayList<>();
        paypalSubs.forEach((id, listPayments) -> {
            subs.add(new Subscription(listPayments));
        });
        stripeSubs.forEach((id, listPayments) -> {
            subs.add(new Subscription(listPayments));
        });
        return subs;
    }

    public Payment getLastPayment(){
        return payments.get(payments.size()-1);
    }

    /**
     * Calculates the amount of milliseconds this subscription is still valid for. <br>
     * If the customer doesn't pay in time, this method returns a negative value. <br>
     * Ignores {@link Payment#timestampCancelled} and relies on {@link Payment#timestampAuthorized}
     * of the last {@link Payment}.
     */
    public long getMillisLeft(){
        Payment lastPayment = getLastPayment();
        long totalMillisValid = Payment.Interval.toMilliseconds(lastPayment.interval);
        return totalMillisValid - (System.currentTimeMillis() - lastPayment.timestampAuthorized);
    }

    /**
     * Same as {@link #getMillisLeft()} but adds the payment processor
     * specific puffer: {@link PayHook#paypalUrlTimeoutMs} or
     * {@link PayHook#stripeUrlTimeoutMs}.
     */
    public long getMillisLeftWithPuffer(){
        //TODO ADD PAYMENT PROCESSOR
        Payment lastPayment = getLastPayment();
        if(lastPayment.stripeSubscriptionId != null)
            return getMillisLeft() + PayHook.stripeUrlTimeoutMs;
        else if(lastPayment.paypalSubscriptionId != null)
            return getMillisLeft() + PayHook.paypalUrlTimeoutMs;
        else
            throw new IllegalStateException("Last payment of subscription must have a payment-processor specific subscription id!");
    }

    public boolean isCancelled(){
        return getLastPayment().timestampCancelled != 0;
    }

    public boolean isPaid(){
        Payment payment = getLastPayment();
        if (payment.timestampAuthorized == 0) return false;
        return getMillisLeft() > 0;
    }
}
