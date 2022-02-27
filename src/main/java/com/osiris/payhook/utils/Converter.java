package com.osiris.payhook.utils;

import com.osiris.payhook.Product;
import com.paypal.api.payments.*;
import com.stripe.model.Price;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converter {

    public Currency toPayPalCurrency(String currency, long priceInSmallestCurrency){
        return new Currency(currency, new BigDecimal(priceInSmallestCurrency).divide(new BigDecimal(100)).toPlainString());
    }

    public Currency toPayPalCurrency(Product product){
        return new Currency(product.currency, new BigDecimal(product.priceInSmallestCurrency).divide(new BigDecimal(100)).toPlainString());
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
            if (product.isCustomBillingInterval()){
                stripeRecurring.setInterval("day");
                stripeRecurring.setIntervalCount((long) product.customBillingIntervallInDays);
            } else{
                stripeRecurring.setInterval("month");
                if (product.isBillingInterval1Month())
                    stripeRecurring.setIntervalCount(1L);
                else if (product.isBillingInterval3Months())
                    stripeRecurring.setIntervalCount(3L);
                else if (product.isBillingInterval6Months())
                    stripeRecurring.setIntervalCount(6L);
                else if (product.isBillingInterval12Months())
                    stripeRecurring.setIntervalCount(12L);
            }
            paramsPrice.put("recurring", stripeRecurring);
        }
        return paramsPrice;
    }

    public com.paypal.api.payments.Plan toPayPalPlan(Product product) {
        com.paypal.api.payments.Plan plan = new Plan(product.name, product.description, "INFINITE");
        plan.setMerchantPreferences(new MerchantPreferences()
                .setAutoBillAmount("YES").setAcceptedPaymentType());
        List<PaymentDefinition> paymentDefinitions = new ArrayList<>(1);
        PaymentDefinition paymentDefinition = new PaymentDefinition().setAmount(toPayPalCurrency(product)).setType("REGULAR");
        if (product.isCustomBillingInterval()){
            paymentDefinition.setFrequency()
            paymentDefinition.setFrequency("DAY");
            paymentDefinition.setCycles(""+product.customBillingIntervallInDays);
        } else{
            paymentDefinition.setFrequency("MONTH");
            if (product.isBillingInterval1Month())
                paymentDefinition.setCycles("1");
            else if (product.isBillingInterval3Months())
                paymentDefinition.setCycles("3");
            else if (product.isBillingInterval6Months())
                paymentDefinition.setCycles("6");
            else if (product.isBillingInterval12Months())
                paymentDefinition.setCycles("12");
        }
        paymentDefinitions.add();
        plan.setPaymentDefinitions(paymentDefinitions);
        return plan;
    }

    public List<Patch> toPayPalPlanPatch(Product product) {
        List<Patch> patches = new ArrayList<>();
        patches.add(new Patch("replace", "name").setValue(product.name));
        patches.add(new Patch("replace", "description").setValue(product.description));
        return patches;
    }


}
