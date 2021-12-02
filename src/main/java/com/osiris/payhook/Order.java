package com.osiris.payhook;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Order {
    private int id;
    private String payUrl;

    // Product related information:
    private long priceInSmallestCurrency;
    private String currency;
    private String name;
    private String description;
    private int billingType; // Returns a value from 0 to 5
    private int customBillingIntervallInDays;
    // Information related to the status of the order:
    private Timestamp lastPaymentTimestamp;
    private Timestamp refundTimestamp;
    private Timestamp cancelTimestamp;

    private final List<Consumer<Event>> actionsOnReceivedPayment = new ArrayList<>();

    public Order(int id, String payUrl, long priceInSmallestCurrency,
                 String currency, String name, String description, int billingType,
                 int customBillingIntervallInDays, Timestamp lastPaymentTimestamp,
                 Timestamp refundTimestamp, Timestamp cancelTimestamp) {
        this.id = id;
        this.payUrl = payUrl;
        this.priceInSmallestCurrency = priceInSmallestCurrency;
        this.currency = currency;
        this.name = name;
        this.description = description;
        this.billingType = billingType;
        this.customBillingIntervallInDays = customBillingIntervallInDays;
        this.lastPaymentTimestamp = lastPaymentTimestamp;
        this.refundTimestamp = refundTimestamp;
        this.cancelTimestamp = cancelTimestamp;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public boolean isRecurring() { // For example a subscription
        return billingType != 0;
    }

    public boolean isBillingInterval1Month() {
        return billingType == 1;
    }

    public boolean isBillingInterval3Months() {
        return billingType == 2;
    }

    public boolean isBillingInterval6Months() {
        return billingType == 3;
    }

    public boolean isBillingInterval12Months() {
        return billingType == 4;
    }

    public boolean isCustomBillingInterval() {
        return billingType == 5;
    }

    public String getFormattedPrice() {
        return priceInSmallestCurrency + " " + currency;
    }

    public long getPriceInSmallestCurrency() {
        return priceInSmallestCurrency;
    }

    public void setPriceInSmallestCurrency(long priceInSmallestCurrency) {
        this.priceInSmallestCurrency = priceInSmallestCurrency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBillingType() {
        return billingType;
    }

    public void setBillingType(int billingType) {
        this.billingType = billingType;
    }

    public int getCustomBillingIntervallInDays() {
        return customBillingIntervallInDays;
    }

    public void setCustomBillingIntervallInDays(int customBillingIntervallInDays) {
        this.customBillingIntervallInDays = customBillingIntervallInDays;
    }

    public Timestamp getLastPaymentTimestamp() {
        return lastPaymentTimestamp;
    }

    public void setLastPaymentTimestamp(Timestamp lastPaymentTimestamp) {
        this.lastPaymentTimestamp = lastPaymentTimestamp;
    }

    public boolean isPaid() {
        return lastPaymentTimestamp != null;
    }

    public boolean isRefunded() {
        return refundTimestamp != null;
    }

    public boolean isCancelled() {
        return cancelTimestamp != null;
    }

    public Timestamp getRefundTimestamp() {
        return refundTimestamp;
    }

    public void setRefundTimestamp(Timestamp refundTimestamp) {
        this.refundTimestamp = refundTimestamp;
    }

    public Timestamp getCancelTimestamp() {
        return cancelTimestamp;
    }

    public void setCancelTimestamp(Timestamp cancelTimestamp) {
        this.cancelTimestamp = cancelTimestamp;
    }
}
