package com.osiris.payhook;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Order {
    public int id;
    public String payUrl;
    private Product[] products;

    // Product related information:
    public long totalPriceInSmallestCurrency;
    public String currency;
    public int customBillingIntervallInDays;
    // Information related to the status of the order:
    public Timestamp creationTimestamp;
    public Timestamp lastPaymentTimestamp;
    public Timestamp refundTimestamp;
    public Timestamp cancelTimestamp;

    private final List<Consumer<Event>> actionsOnReceivedPayment = new ArrayList<>();

    /**
     * Make sure that the {@link #totalPriceInSmallestCurrency} matches the total price of all passed over {@link #products}.
     */
    public Order(int id, String payUrl, long totalPriceInSmallestCurrency,
                 String currency, int customBillingIntervallInDays, Timestamp creationTimestamp, Timestamp lastPaymentTimestamp,
                 Timestamp refundTimestamp, Timestamp cancelTimestamp, Product[] products) {
        this.id = id;
        this.payUrl = payUrl;
        this.totalPriceInSmallestCurrency = totalPriceInSmallestCurrency;
        this.currency = currency;
        this.customBillingIntervallInDays = customBillingIntervallInDays;
        this.creationTimestamp = creationTimestamp;
        this.lastPaymentTimestamp = lastPaymentTimestamp;
        this.refundTimestamp = refundTimestamp;
        this.cancelTimestamp = cancelTimestamp;
        this.products = products;
    }

    public void onPaymentReceived(Consumer<Event> action) {
        synchronized (actionsOnReceivedPayment){
            actionsOnReceivedPayment.add(action);
        }
    }

    public void executePaymentReceived(){
        synchronized (actionsOnReceivedPayment){
            for (Consumer<Event> action :
                    actionsOnReceivedPayment) {
                action.accept(new Event(this));
            }
            actionsOnReceivedPayment.clear();
            // TODO LINK THIS WITH ACTUAL WEBHOOK EVENTS
        }
    }
}
