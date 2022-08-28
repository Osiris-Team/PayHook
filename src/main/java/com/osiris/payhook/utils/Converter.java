package com.osiris.payhook.utils;

import com.osiris.payhook.Product;
import com.paypal.api.payments.MerchantPreferences;
import com.paypal.api.payments.PaymentDefinition;
import com.paypal.payments.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converter {

    public long toSmallestCurrency(com.paypal.orders.Money amount) {
        return new BigDecimal(amount.value()).multiply(new BigDecimal(100)).longValue();
    }

    public long toSmallestCurrency(Money amount) {
        return new BigDecimal(amount.value()).multiply(new BigDecimal(100)).longValue();
    }

    public Money toPayPalMoney(String currency, long priceInSmallestCurrency) {
        return new Money().currencyCode(currency).value(new BigDecimal(priceInSmallestCurrency).divide(new BigDecimal(100)).toPlainString());
    }

    public com.paypal.api.payments.Currency toPayPalCurrency(String currency, long priceInSmallestCurrency) {
        return new com.paypal.api.payments.Currency(currency, new BigDecimal(priceInSmallestCurrency).divide(new BigDecimal(100)).toPlainString());
    }

    public com.paypal.api.payments.Currency toPayPalCurrency(Product product) {
        return new com.paypal.api.payments.Currency(product.currency, new BigDecimal(product.charge).divide(new BigDecimal(100)).toPlainString());
    }

    public Map<String, Object> toStripeProduct(Product product) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", product.name);
        params.put("description", product.description);
        return params;
    }

    public Map<String, Object> toStripePrice(Product product) {
        Map<String, Object> paramsPrice = new HashMap<>();
        paramsPrice.put("currency", product.currency);
        paramsPrice.put("product", product.stripeProductId);
        paramsPrice.put("unit_amount", product.charge);
        if (product.isRecurring()) {
            Map<String, Object> recurring = new HashMap<>();
            recurring.put("interval", "day");
            if (product.paymentInterval <= 0)
                throwInvalidPaymentInterval(product.paymentInterval);
            recurring.put("interval_count", (long) product.paymentInterval);
            recurring.put("usage_type", "licensed");

            paramsPrice.put("recurring", recurring);
        }
        return paramsPrice;
    }

    public com.paypal.api.payments.Plan toPayPalPlan(Product product, String successUrl, String cancelUrl) {
        com.paypal.api.payments.Plan plan = new com.paypal.api.payments.Plan(product.name, product.description, "INFINITE");
        plan.setMerchantPreferences(new MerchantPreferences()
                        .setReturnUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                .setAutoBillAmount("YES"));
        List<PaymentDefinition> paymentDefinitions = new ArrayList<>(1);
        PaymentDefinition paymentDefinition = new PaymentDefinition()
                .setName("Payment for "+product.name)
                .setAmount(toPayPalCurrency(product))
                .setType("REGULAR");
        paymentDefinition.setFrequency("DAY");
        if (product.paymentInterval <= 0)
            throwInvalidPaymentInterval(product.paymentInterval);
        paymentDefinition.setFrequencyInterval("" + product.paymentInterval);

        paymentDefinitions.add(paymentDefinition);
        plan.setPaymentDefinitions(paymentDefinitions);
        return plan;
    }

    private void throwInvalidPaymentInterval(int paymentInterval) {
        throw new IllegalArgumentException("Payment interval ("+paymentInterval+") cannot be <= 0 if product is meant to have recurring payments!");
    }

    public List<com.paypal.api.payments.Patch> toPayPalPlanPatch(Product product) {
        List<com.paypal.api.payments.Patch> patches = new ArrayList<>();
        patches.add(new com.paypal.api.payments.Patch("replace", "name").setValue(product.name));
        patches.add(new com.paypal.api.payments.Patch("replace", "description").setValue(product.description));
        return patches;
    }


}
