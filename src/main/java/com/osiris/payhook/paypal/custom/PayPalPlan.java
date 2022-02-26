/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.autoplug.core.json.exceptions.WrongJsonTypeException;
import com.osiris.autoplug.core.logger.AL;

import java.io.IOException;

import static com.osiris.autoplug.webserver.payment.paypal.custom.MyPayPalAPI.BASE_URL;

public class PayPalPlan {
    private final PayPalREST context;
    private final String planId;
    private final String productId;
    private final String name;
    private final String description;
    private final Status status;

    public PayPalPlan(PayPalREST context, String planId, String productId, String name, String description, Status status) {
        this.context = context;
        this.planId = planId;
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    /**
     * Creates a subscription for this plan and returns it.
     */
    public PayPalSubscription createSubscription(String brandName, String returnUrl, String cancelUrl) throws WrongJsonTypeException, IOException, HttpErrorException {
        JsonObject obj = new JsonObject();
        obj.addProperty("plan_id", planId);

        JsonObject applicationContext = new JsonObject();
        obj.add("application_context", applicationContext);
        applicationContext.addProperty("brand_name", brandName);
        applicationContext.addProperty("locale", "en-US");
        applicationContext.addProperty("shipping_preference", "NO_SHIPPING");
        applicationContext.addProperty("user_action", "SUBSCRIBE_NOW");

        JsonObject paymentMethod = new JsonObject();
        applicationContext.add("payment_method", paymentMethod);
        paymentMethod.addProperty("payer_selected", "PAYPAL");
        paymentMethod.addProperty("payee_preferred", "IMMEDIATE_PAYMENT_REQUIRED");

        applicationContext.addProperty("return_url", returnUrl);
        applicationContext.addProperty("cancel_url", cancelUrl);


        JsonObject resultObj = new UtilsPayPalJson().postJsonAndGetResponse(BASE_URL + "/billing/subscriptions", obj, context, 201)
                .getAsJsonObject();

        String approveUrl = null;
        String editUrl = null;
        String selfUrl = null;
        for (JsonElement element :
                resultObj.get("links").getAsJsonArray()) {
            if (element.getAsJsonObject().get("rel").getAsString().equals("approve"))
                approveUrl = element.getAsJsonObject().get("href").getAsString();
            if (element.getAsJsonObject().get("rel").getAsString().equals("edit"))
                editUrl = element.getAsJsonObject().get("href").getAsString();
            if (element.getAsJsonObject().get("rel").getAsString().equals("self"))
                selfUrl = element.getAsJsonObject().get("href").getAsString();
            else
                AL.warn("Couldn't determine url type: " + element.getAsJsonObject().get("rel").getAsString());
        }
        return new PayPalSubscription(this,
                resultObj.get("id").getAsString(),
                new UtilsPayPal().getSubscriptionStatus(resultObj.get("status").getAsString()),
                approveUrl,
                editUrl,
                selfUrl);
    }

    public String getPlanId() {
        return planId;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        CREATED, ACTIVE, INACTIVE
    }
}
