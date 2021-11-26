package com.osiris.payhook.paypal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.osiris.payhook.PayHook;

import java.util.List;


/**
 * The in-memory representation of a Webhook event/notification. <br>
 * Can be validated through {@link PayHook#validateWebhookEvent(PaypalWebhookEvent)}.
 */
public class PaypalWebhookEvent {
    private final String validWebhookId;
    private final List<String> validTypesList;
    private final String bodyString;
    private final PaypalWebhookEventHeader header;
    private final JsonObject body;
    private boolean isValid = false;


    /**
     * The in-memory representation of a Webhook event/notification. <br>
     * Can be validated through {@link PayHook#validateWebhookEvent(PaypalWebhookEvent)}.
     *
     * @param validWebhookId your webhooks valid id. Get it from here: https://developer.paypal.com/developer/applications/
     * @param validTypesList your webhooks valid types/names. Here is a full list: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names/
     * @param header         the http messages header as {@link PaypalWebhookEventHeader}.
     * @param body           the http messages body as {@link JsonObject}.
     */
    public PaypalWebhookEvent(String validWebhookId, List<String> validTypesList, PaypalWebhookEventHeader header, JsonObject body) {
        this.validWebhookId = validWebhookId;
        this.validTypesList = validTypesList;
        this.header = header;
        this.body = body;
        this.bodyString = new Gson().toJson(body);
    }

    public String getValidWebhookId() {
        return validWebhookId;
    }

    public List<String> getValidTypesList() {
        return validTypesList;
    }

    public String getBodyString() {
        return bodyString;
    }

    public PaypalWebhookEventHeader getHeader() {
        return header;
    }

    public JsonObject getBody() {
        return body;
    }

    /**
     * Perform {@link PayHook#validateWebhookEvent(PaypalWebhookEvent)} on this event, so this method
     * returns the right value.
     *
     * @return true if this event is a valid paypal webhook event.
     */
    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    /**
     * Shortcut for returning the id from the json body.
     */
    public String getId() {
        return body.get("id").getAsString();
    }

    /**
     * Shortcut for returning the summary from the json body.
     */
    public String getSummary() {
        return body.get("summary").getAsString();
    }

    /**
     * Shortcut for returning the event_type from the json body.
     */
    public String getEventType() {
        return body.get("event_type").getAsString();
    }

    /**
     * Shortcut for returning the resource_type from the json body.
     */
    public String getResourceType() {
        return body.get("resource_type").getAsString();
    }

    /**
     * Shortcut for returning the event_version from the json body.
     */
    public String getEventVersion() {
        return body.get("event_version").getAsString();
    }

    /**
     * Shortcut for returning the event_version from the json body.
     */
    public String getResourceVersion() {
        return body.get("resource_version").getAsString();
    }

}
