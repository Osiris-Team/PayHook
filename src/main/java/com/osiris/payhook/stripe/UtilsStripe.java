package com.osiris.payhook.stripe;

import com.google.gson.JsonSyntaxException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import java.util.Map;

public class UtilsStripe {

    /**
     * Checks if the provided webhook event is actually a real/valid one.
     * @return null, if not valid.
     */
    public Event checkWebhookEvent(String body, Map<String, String> headers, String endpointSecret){
        String sigHeader = headers.get("Stripe-Signature");
        if(sigHeader==null) return null;

        Event event = null;
        try {
            event = Webhook.constructEvent(body, sigHeader, endpointSecret);
        } catch (JsonSyntaxException | SignatureVerificationException e) {
            return null; // Invalid payload // Invalid signature
        }
        return event;
    }

}
