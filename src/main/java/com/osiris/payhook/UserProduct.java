package com.osiris.payhook;

/**
 * Can be bought by a consumer. <br>
 * Supports multiple {@link PaymentProcessor}s, one-time and recurring payments. <br>
 */
public class UserProduct {
    public final int userProductId;
    public final int productId;
    public final int orderId;
    public final int paymentId;
    public long priceInSmallestCurrency;
    public String currency;
    public String name;
    public String description;
    public int billingType; // Returns a value from 0 to 5
    public int customBillingIntervallInDays;

    // PayPal specific stuff:
    public String paypalProductId; // TODO Create java API for Create/Update/Retrieve
    public String paypalPlanId; // TODO

    // Stripe specific stuff:
    public String stripeProductId;
    public String stripePriceId;

    public UserProduct(int id, long priceInSmallestCurrency,
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
}
