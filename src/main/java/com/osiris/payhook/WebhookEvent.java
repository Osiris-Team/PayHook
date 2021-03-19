package com.osiris.payhook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.osiris.payhook.exceptions.ParseBodyException;

import java.util.List;

public class WebhookEvent {
    private String validWebHookId;
    private List<String> validTypesList;
    private String bodyString;
    private WebhookEventHeader header;
    private JsonObject body;

    /**
     * The in-memory representation of a webhook event.
     * @param validWebHookId your webhooks valid id. Get it from here: https://developer.paypal.com/developer/applications/
     * @param validTypesList your webhooks valid types/names. Here is a full list: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names/
     * @param header the http messages header as {@link WebhookEventHeader}.
     * @param body the http messages body as {@link JsonObject}.
     */
    public WebhookEvent(String validWebHookId, List<String> validTypesList, WebhookEventHeader header, JsonObject body) {
        this.validWebHookId = validWebHookId;
        this.validTypesList = validTypesList;
        this.header = header;
        this.body = body;
        this.bodyString = new Gson().toJson(body);
    }

    /**
     * The in-memory representation of a webhook event.
     * @param validWebHookId your webhooks valid id. Get it from here: https://developer.paypal.com/developer/applications/
     * @param validTypesList your webhooks valid types/names. Here is a full list: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names/
     * @param header the http messages header as {@link WebhookEventHeader}.
     * @param bodyString the http messages body as {@link String}.
     * @throws ParseBodyException
     */
    public WebhookEvent(String validWebHookId, List<String> validTypesList, WebhookEventHeader header, String bodyString) throws ParseBodyException {
        this.validWebHookId = validWebHookId;
        this.validTypesList = validTypesList;
        this.header = header;
        this.bodyString = bodyString;
        this.body = new PayHook().parseAndGetBody(bodyString);
    }

    public String getValidWebHookId() {
        return validWebHookId;
    }

    public List<String> getValidTypesList() {
        return validTypesList;
    }

    public String getBodyString() {
        return bodyString;
    }

    public WebhookEventHeader getHeader() {
        return header;
    }

    public JsonObject getBody() {
        return body;
    }
}
