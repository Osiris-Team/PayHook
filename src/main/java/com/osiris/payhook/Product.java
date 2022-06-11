package com.osiris.payhook;

/**
 * Represents a digital good or service. <br>
 * Supports multiple {@link PaymentProcessor}s and multiple payment methods: one-time and recurring payments. <br>
 */
public class Product {
    public final int id;
    public long charge;
    /**
     * The <a href="https://de.wikipedia.org/wiki/ISO_4217">ISO_4217</a> currency code.
     */
    public String currency;
    public String name;
    public String description;
    public Payment.Intervall paymentIntervall; // Returns a value from 0 to 5
    /**
     * The intervall in days until the next due {@link Payment}. <br>
     * Only relevant when {@link #paymentIntervall} is {@link PaymentType#RECURRING_CUSTOM}. <br>
     */
    public int customPaymentIntervall;

    // PayPal specific stuff:
    public String paypalProductId;
    public String paypalPlanId; // TODO

    // Stripe specific stuff:
    public String stripeProductId;
    public String stripePriceId;

    public Product(int id, long charge,
                   String currency, String name, String description,
                   Payment.Intervall paymentIntervall, int customPaymentIntervall,
                   String paypalProductId, String stripeProductId) {
        this.id = id;
        this.charge = charge;
        this.currency = currency;
        this.name = name;
        this.description = description;
        this.paymentIntervall = paymentIntervall;
        this.customPaymentIntervall = customPaymentIntervall;
        this.paypalProductId = paypalProductId;
        this.stripeProductId = stripeProductId;
    }

    public boolean isPayPalSupported(){
        return paypalProductId != null;
    }

    public boolean isBraintreeSupported(){
        return false; // TODO
    }

    public boolean isStripeSupported(){
        return stripeProductId != null && stripePriceId != null;
    }

    public boolean isRecurring() { // For example a subscription
        return paymentIntervall != Payment.Intervall.NONE;
    }

    public boolean isBillingInterval1Month() {
        return paymentIntervall == Payment.Intervall.DAYS_30;
    }

    public boolean isBillingInterval3Months() {
        return paymentIntervall == Payment.Intervall.DAYS_90;
    }

    public boolean isBillingInterval6Months() {
        return paymentIntervall == Payment.Intervall.DAYS_180;
    }

    public boolean isBillingInterval12Months() {
        return paymentIntervall == Payment.Intervall.DAYS_360;
    }

    public boolean isCustomBillingInterval() {
        return paymentIntervall == Payment.Intervall.DAYS_CUSTOM;
    }

    public String getFormattedPrice() {
        return charge + " " + currency;
    }
}
