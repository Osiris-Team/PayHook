package com.osiris.payhook;

/**
 * Represents a digital good or service. <br>
 * Supports multiple {@link PaymentProcessor}s and multiple payment methods: one-time and recurring payments. <br>
 */
public class Product {
    public final int id;
    public long priceInSmallestCurrency;
    /**
     * The <a href="https://de.wikipedia.org/wiki/ISO_4217">ISO_4217</a> currency code.
     */
    public String currency;
    public String name;
    public String description;
    public PaymentType paymentType; // Returns a value from 0 to 5
    /**
     * The intervall in days until the next due {@link Payment}. <br>
     * Only relevant when {@link #paymentType} is {@link PaymentType#RECURRING_CUSTOM}. <br>
     */
    public int customPaymentIntervall;

    // PayPal specific stuff:
    public String braintreeProductId;
    public String braintreePlanId; // TODO

    // Stripe specific stuff:
    public String stripeProductId;
    public String stripePriceId;

    public Product(int id, long priceInSmallestCurrency,
                   String currency, String name, String description,
                   PaymentType paymentType, int customPaymentIntervall,
                   String braintreeProductId, String stripeProductId) {
        this.id = id;
        this.priceInSmallestCurrency = priceInSmallestCurrency;
        this.currency = currency;
        this.name = name;
        this.description = description;
        this.paymentType = paymentType;
        this.customPaymentIntervall = customPaymentIntervall;
        this.braintreeProductId = braintreeProductId;
        this.stripeProductId = stripeProductId;
    }

    public boolean isBraintreeSupported(){
        return braintreeProductId != null;
    }

    public boolean isStripeSupported(){
        return stripeProductId != null && stripePriceId != null;
    }

    public boolean isRecurring() { // For example a subscription
        return paymentType != PaymentType.ONE_TIME;
    }

    public boolean isBillingInterval1Month() {
        return paymentType == PaymentType.RECURRING;
    }

    public boolean isBillingInterval3Months() {
        return paymentType == PaymentType.RECURRING_3;
    }

    public boolean isBillingInterval6Months() {
        return paymentType == PaymentType.RECURRING_6;
    }

    public boolean isBillingInterval12Months() {
        return paymentType == PaymentType.RECURRING_12;
    }

    public boolean isCustomBillingInterval() {
        return paymentType == PaymentType.RECURRING_CUSTOM;
    }

    public String getFormattedPrice() {
        return priceInSmallestCurrency + " " + currency;
    }
}
