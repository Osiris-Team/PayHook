package com.osiris.payhook;

public class Product {
    private final int id;
    private long priceInSmallestCurrency;
    private String currency;
    private String name;
    private String description;
    private int billingType; // Returns a value from 0 to 5
    private int customBillingIntervallInDays;
    private String paypalProductId;
    private String stripeProductId;

    public Product(int id, long priceInSmallestCurrency,
                   String currency, String name, String description,
                   int billingType, int customBillingIntervallInDays,
                   String paypalProductId, String stripeProductId) {
        this.id = id;
        this.priceInSmallestCurrency = priceInSmallestCurrency;
        this.currency = currency;
        this.name = name;
        this.description = description;
        this.billingType = billingType;
        this.customBillingIntervallInDays = customBillingIntervallInDays;
        this.paypalProductId = paypalProductId;
        this.stripeProductId = stripeProductId;
    }

    public int getId() {
        return id;
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

    public String getPaypalProductId() {
        return paypalProductId;
    }

    public void setPaypalProductId(String paypalProductId) {
        this.paypalProductId = paypalProductId;
    }

    public String getStripeProductId() {
        return stripeProductId;
    }

    public void setStripeProductId(String stripeProductId) {
        this.stripeProductId = stripeProductId;
    }
}
