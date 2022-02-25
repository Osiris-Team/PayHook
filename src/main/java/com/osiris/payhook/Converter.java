package com.osiris.payhook;

import com.paypal.api.payments.MerchantPreferences;
import com.paypal.api.payments.Patch;
import com.paypal.api.payments.Plan;
import com.stripe.model.Price;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converter {

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
        plan.setMerchantPreferences(new MerchantPreferences())
        return plan;
    }

    public List<Patch> toPayPalPlanPatch(Product product) {
        List<Patch> patches = new ArrayList<>();
        patches.add(new Patch("replace", "name").setValue(product.name));
        patches.add(new Patch("replace", "description").setValue(product.description));
        return patches;
    }


}
