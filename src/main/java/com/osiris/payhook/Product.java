package com.osiris.payhook;

/**
 * Can be bought by a consumer. <br>
 * Supports multiple {@link PaymentProcessor}s, one-time and recurring payments. <br>
 */
public class Product {
    public final int productId;
    public long priceInSmallestCurrency;
    public String currency;
    public String name;
    public String description;
    public PaymentType paymentType; // Returns a value from 0 to 5
    public int customBillingIntervallInDays;

    // PayPal specific stuff:
    public String paypalProductId;
    public String paypalPlanId; // TODO

    // Stripe specific stuff:
    public String stripeProductId;
    public String stripePriceId;

    public Product(int productId, long priceInSmallestCurrency,
                   String currency, String name, String description,
                   PaymentType paymentType, int customBillingIntervallInDays,
                   String paypalProductId, String stripeProductId) {
        this.productId = productId;
        this.priceInSmallestCurrency = priceInSmallestCurrency;
        this.currency = currency;
        this.name = name;
        this.description = description;
        this.paymentType = paymentType;
        this.customBillingIntervallInDays = customBillingIntervallInDays;
        this.paypalProductId = paypalProductId;
        this.stripeProductId = stripeProductId;
    }

    /**
     * Returns true when this product has all the necessary PayPal related information available.
     */
    public boolean isPayPalSupported(){
        return paypalProductId != null;
    }

    /**
     * Returns true when this product has all the necessary Stripe related information available.
     */
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
