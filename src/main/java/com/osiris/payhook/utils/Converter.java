package com.osiris.payhook.utils;

import com.osiris.payhook.Product;
import com.paypal.api.payments.MerchantPreferences;
import com.paypal.api.payments.PaymentDefinition;
import com.stripe.model.Price;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converter {

    public com.paypal.api.payments.Currency toPayPalCurrency(String currency, long priceInSmallestCurrency){
        return new com.paypal.api.payments.Currency(currency, new BigDecimal(priceInSmallestCurrency).divide(new BigDecimal(100)).toPlainString());
    }

    public com.paypal.api.payments.Currency toPayPalCurrency(Product product){
        return new com.paypal.api.payments.Currency(product.currency, new BigDecimal(product.charge).divide(new BigDecimal(100)).toPlainString());
    }

    public Map<String, Object> toStripeProduct(Product product, boolean isStripeSandbox){
        Map<String, Object> params = new HashMap<>();
        params.put("name", product.name);
        params.put("description", product.description);
        params.put("livemode", isStripeSandbox);
        return params;
    }

    public Map<String, Object> toStripePrice(Product product) {
        Map<String, Object> paramsPrice = new HashMap<>();
        paramsPrice.put("currency", product.currency);
        paramsPrice.put("product", product.stripeProductId);
        if (product.isRecurring()){
            Price.Recurring stripeRecurring = new Price.Recurring();
            stripeRecurring.setInterval("day");
            if (product.isCustomBillingInterval()) stripeRecurring.setIntervalCount((long) product.customPaymentIntervall);
            else if (product.isBillingInterval1Month()) stripeRecurring.setIntervalCount(30L);
            else if (product.isBillingInterval3Months()) stripeRecurring.setIntervalCount(90L);
            else if (product.isBillingInterval6Months()) stripeRecurring.setIntervalCount(180L);
            else if (product.isBillingInterval12Months()) stripeRecurring.setIntervalCount(360L);
            else throw new IllegalArgumentException("Unknown/Null billing intervall!");
            paramsPrice.put("recurring", stripeRecurring);
        }
        return paramsPrice;
    }

    public com.paypal.api.payments.Plan toPayPalPlan(Product product) {
        com.paypal.api.payments.Plan plan = new com.paypal.api.payments.Plan(product.name, product.description, "INFINITE");
        plan.setMerchantPreferences(new MerchantPreferences()
                .setAutoBillAmount("YES"));
        List<PaymentDefinition> paymentDefinitions = new ArrayList<>(1);
        PaymentDefinition paymentDefinition = new PaymentDefinition().setAmount(toPayPalCurrency(product)).setType("REGULAR");
        paymentDefinition.setFrequency("DAY");
        if (product.isCustomBillingInterval()) paymentDefinition.setFrequencyInterval(""+product.customPaymentIntervall);
        else if (product.isBillingInterval1Month()) paymentDefinition.setFrequencyInterval("30");
        else if (product.isBillingInterval3Months()) paymentDefinition.setFrequencyInterval("90");
        else if (product.isBillingInterval6Months()) paymentDefinition.setFrequencyInterval("180");
        else if (product.isBillingInterval12Months()) paymentDefinition.setFrequencyInterval("360");
        else throw new IllegalArgumentException("Unknown/Null billing intervall!");

        paymentDefinitions.add(paymentDefinition);
        plan.setPaymentDefinitions(paymentDefinitions);
        return plan;
    }

    public List<com.paypal.api.payments.Patch> toPayPalPlanPatch(Product product) {
        List<com.paypal.api.payments.Patch> patches = new ArrayList<>();
        patches.add(new com.paypal.api.payments.Patch("replace", "name").setValue(product.name));
        patches.add(new com.paypal.api.payments.Patch("replace", "description").setValue(product.description));
        return patches;
    }

}
