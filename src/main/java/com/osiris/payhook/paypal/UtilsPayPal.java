/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.ParseHeaderException;

import java.util.Map;
import java.util.Objects;

public class UtilsPayPal {

    public PayPalPlan.Status getPlanStatus(String statusAsString) {
        if (statusAsString.equalsIgnoreCase(PayPalPlan.Status.ACTIVE.name()))
            return PayPalPlan.Status.ACTIVE;
        else if (statusAsString.equalsIgnoreCase(PayPalPlan.Status.INACTIVE.name()))
            return PayPalPlan.Status.INACTIVE;
        else if (statusAsString.equalsIgnoreCase(PayPalPlan.Status.CREATED.name()))
            return PayPalPlan.Status.CREATED;
        else
            return null;
    }

    public PayPalSubscription.Status getSubscriptionStatus(String statusAsString) {
        if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.APPROVAL_PENDING.name()))
            return PayPalSubscription.Status.APPROVAL_PENDING;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.APPROVED.name()))
            return PayPalSubscription.Status.APPROVED;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.ACTIVE.name()))
            return PayPalSubscription.Status.ACTIVE;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.SUSPENDED.name()))
            return PayPalSubscription.Status.SUSPENDED;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.CANCELLED.name()))
            return PayPalSubscription.Status.CANCELLED;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.EXPIRED.name()))
            return PayPalSubscription.Status.EXPIRED;
        else
            return null;
    }

    /**
     * Parses the provided header {@link Map}
     * into a {@link PaypalWebhookEventHeader} object and returns it.
     */
    public PaypalWebhookEventHeader parseAndGetHeader(Map<String, String> headerAsMap, String webhookId) throws ParseHeaderException {
        // Check if all keys we need exist
        String transmissionId = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_ID);
        String timestamp = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_TIME);
        String transmissionSignature = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_SIG);
        String certUrl = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_CERT_URL);
        String authAlgorithm = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_AUTH_ALGO);
        return new PaypalWebhookEventHeader(transmissionId, timestamp, transmissionSignature, authAlgorithm, certUrl, webhookId);
    }

    /**
     * Parses the provided body {@link String}
     * into a {@link JsonObject} and returns it.
     */
    public JsonObject parseAndGetBody(String bodyAsString) throws ParseBodyException {
        try {
            return JsonParser.parseString(bodyAsString).getAsJsonObject();
        } catch (Exception e) {
            throw new ParseBodyException(e.getMessage());
        }
    }

    /**
     * Checks if the provided key exists in the provided map and returns its value. <br>
     * The keys existence is checked by {@link String#equalsIgnoreCase(String)}, so that its case is ignored. <br>
     *
     * @return the value mapped to the provided key.
     */
    public String checkKeyAndGetValue(Map<String, String> map, String key) throws ParseHeaderException {
        Objects.requireNonNull(map);
        Objects.requireNonNull(key);

        String value = map.get(key);
        if (value == null || value.equals("")) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }

            if (value == null || value.equals("")) {
                throw new ParseHeaderException("Header is missing the '" + key + "' key or its value!");
            }
        }
        return value;
    }

}
