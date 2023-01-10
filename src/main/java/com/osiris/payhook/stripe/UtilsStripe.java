package com.osiris.payhook.stripe;

import com.google.gson.JsonSyntaxException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import java.util.Map;

public class UtilsStripe {

    /**
     * Checks if the provided webhook event is actually a real/valid one.
     *
     * @return null, if not valid.
     */
    public Event checkWebhookEvent(String body, Map<String, String> headers, String endpointSecret)
            throws JsonSyntaxException, SignatureVerificationException { // Invalid payload // Invalid signature
        String sigHeader = headers.get("Stripe-Signature");
        if (sigHeader == null) {
            sigHeader = headers.get("stripe-signature"); // try lowercase
            if(sigHeader == null)
                throw new SignatureVerificationException("No Stripe-Signature/stripe-signature header present!", "---");
        }
        return Webhook.constructEvent(body, sigHeader, endpointSecret);
    }

}
