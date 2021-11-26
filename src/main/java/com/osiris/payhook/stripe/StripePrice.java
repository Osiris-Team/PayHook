package com.osiris.payhook.stripe;

public class StripePrice {
    private long priceInSmallestCurrency;
    private String currency;

    public StripePrice(long priceInSmallestCurrency, String currency) {
        this.priceInSmallestCurrency = priceInSmallestCurrency;
        this.currency = currency;
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
}
