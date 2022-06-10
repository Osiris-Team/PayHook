package com.osiris.payhook.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadWithCounter extends Thread{
    public AtomicInteger counter = new AtomicInteger(0);
    public Runnable runnable = null;

    @Override
    public void run() {
        super.run();
        if(runnable!=null) runnable.run();
    }
    /*
    TODO Support this:
            ThreadWithCounter threadCheckIfPaid = new ThreadWithCounter(); // Only ran if no webhook
        threadCheckIfPaid.runnable = () -> {
            try{
                long msToSleepStart = 10000; // 10sek
                long msToSleep = msToSleepStart;
                long msSleptTotal = 0;
                while (true){
                    Thread.sleep(msToSleep);
                    msSleptTotal += msToSleep;

                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    Map<String, List<Payment>> paypalOrders = new HashMap<>();
                    // CAPTURE PAYPAL SUBSCRIPTIONS:
                    for (Payment pendingPayment : database.getPendingPayments()) {
                        if(pendingPayment.isPayPalSupported()){
                            try{
                                String paypalCaptureId = null;
                                if(pendingPayment.isRecurring()){
                                    JsonObject obj = myPayPal.captureSubscription(
                                            pendingPayment.paypalSubscriptionId,
                                            new Converter().toPayPalCurrency(pendingPayment));
                                    // TODO update db on success
                                }
                                else {
                                    Objects.requireNonNull(pendingPayment.paypalOrderId);
                                    if(paypalOrders.get(pendingPayment.paypalOrderId) == null)
                                        paypalOrders.put(pendingPayment.paypalOrderId, new ArrayList<>());
                                    paypalOrders.get(pendingPayment.paypalOrderId)
                                            .add(pendingPayment);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }

                    // CAPTURE PAYPAL ORDERS:
                    for (List<Payment> order : paypalOrders.values()) {
                        long fullCharge = 0;
                        for (Payment p: order) {
                            fullCharge += p.charge;
                        }
                        Payment pFirst = order.get(0);
                        try{
                            String captureId = myPayPal.captureOrder(paypalV2, pFirst.paypalOrderId,
                                    new Converter().toPayPalCurrency(pFirst.currency, fullCharge));
                            for (Payment p : order) {
                                p.paypalCaptureId = captureId;
                                p.timestampPaid = now;
                                database.updatePayment(p);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    msToSleep += msToSleepStart;
                    threadCheckIfPaid.counter.incrementAndGet();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
     */
}
