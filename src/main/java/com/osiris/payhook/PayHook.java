package com.osiris.payhook;

import com.google.gson.*;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.ParseHeaderException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.Constants;
import com.osiris.payhook.paypal.SSLUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PayHook {
    private boolean isSandboxMode = false;

    /**
     * Parses the header, represented as {@link Map},
     * into a {@link WebhookEventHeader} object and returns it.
     * @throws ParseHeaderException if this operation fails.
     */
    public WebhookEventHeader parseAndGetHeader(Map<String, String> headerAsMap) throws ParseHeaderException {
        // Check if all keys we need exist
        String transmissionId        = validateKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_ID);
        String timestamp             = validateKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_TIME);
        String transmissionSignature = validateKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_SIG);
        String certUrl               = validateKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_CERT_URL);
        String authAlgorithm         = validateKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_AUTH_ALGO);

        // Note that the webhook id and crc32 get set after the validation was run
        return new WebhookEventHeader(transmissionId, timestamp, transmissionSignature, authAlgorithm, certUrl);
    }

    /**
     * Parses the body, represented as {@link String},
     * into a {@link JsonObject} and returns it.
     * @throws ParseBodyException if this operation fails.
     */
    public JsonObject parseAndGetBody(String bodyString) throws ParseBodyException {
        try{
            return JsonParser.parseString(bodyString).getAsJsonObject();
        } catch (Exception e) {
            throw new ParseBodyException(e.getMessage());
        }
    }

    /**
     * Checks if the provided key exists in the map and returns its value.
     * The keys existence is checked by {@link String#equalsIgnoreCase(String)}, so that its case is ignored.
     * @return the value mapped to the provided key.
     * @throws WebHookValidationException
     */
    public String validateKeyAndGetValue(Map<String, String> map, String key) throws ParseHeaderException {
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
                throw new ParseHeaderException("Header is missing the '"+key+"' key or its value!");
            }
        }
        return value;
    }

    /**
     * See {@link #validateWebhookEvent(WebhookEvent)} (WebHookEvent)} for details.
     * @param validId your webhooks valid id. Get it from here: https://developer.paypal.com/developer/applications/
     * @param validTypes your webhooks valid types/names. Here is a full list: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names/
     * @param header the http messages header as string.
     * @param body the http messages body as string.
     */
    public void validateWebhookEvent(String validId, List<String> validTypes, WebhookEventHeader header, String body) throws ParseBodyException, WebHookValidationException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, SignatureException, InvalidKeyException {
        validateWebhookEvent(new WebhookEvent(validId, validTypes, header, body));
    }

    /**
     * Performs various checks to see if the received {@link WebhookEvent} is valid or not.
     * Performed checks:
     * Is this events name/type in the valid events list?
     * Are this events certificates valid?
     * Is this events data/transmission-signature valid?
     * Do the webhook ids match?
     * @param event
     * @return true if the webhook event is valid
     * @throws WebHookValidationException if validation failed. IMPORTANT: MESSAGE MAY CONTAIN SENSITIVE INFORMATION!
     * @throws ParseBodyException
     */
    public void validateWebhookEvent(WebhookEvent event) throws WebHookValidationException, ParseBodyException, IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        WebhookEventHeader header = event.getHeader();

        // Check if the webhook types match
        List<String> validEventTypes = event.getValidTypesList();

        // event_type can be either an json array or a normal field. Do stuff accordingly.
        JsonElement elementEventType = event.getBody().get("event_type");
        if (elementEventType==null) elementEventType = event.getBody().get("event_types"); // Check for event_types
        if (elementEventType==null) throw new ParseBodyException("Failed to find key 'event_type' or 'event_types' in the provided json body."); // if the element is still null

        if (elementEventType.isJsonArray()){
            // This means we have multiple event_type objects in the array
            JsonArray arrayEventType = elementEventType.getAsJsonArray();
            for (JsonElement singleElementEventType :
                    arrayEventType) {
                JsonObject o = singleElementEventType.getAsJsonObject();
                if (!validEventTypes.contains(o.get("name").getAsString()))
                    throw new WebHookValidationException("No valid type("+o.get("name")+") found in the valid types list: "+validEventTypes.toString());
            }
        }
        else{
            // This means we only have one event_type in the json and not an array.
            String webHookType = event.getBody().get("event_type").getAsString();
            if (!validEventTypes.contains(webHookType))
                throw new WebHookValidationException("No valid type("+webHookType+") found in the valid types list: "+validEventTypes.toString());
        }

        // Load certs
        String clientCertificateLocation = event.getHeader().getCertUrl();
        String trustCertificateLocation = Constants.PAYPAL_TRUST_DEFAULT_CERT;
        Collection<X509Certificate> clientCerts = SSLUtil.getCertificateFromStream(new BufferedInputStream(new URL(clientCertificateLocation).openStream()));
        Collection<X509Certificate> trustCerts = SSLUtil.getCertificateFromStream(PayHook.class.getClassLoader().getResourceAsStream(trustCertificateLocation));

        // Check the chain
        SSLUtil.validateCertificateChain(clientCerts, trustCerts, "RSA");

        // Construct expected signature
        String validWebhookId           = event.getValidWebhookId();
        String actualEncodedSignature   = header.getTransmissionSignature();
        String authAlgo                 = header.getAuthAlgorithm();
        String transmissionId           = header.getTransmissionId();
        String transmissionTime         = header.getTimestamp();
        String bodyString               = event.getBodyString();
        String expectedDecodedSignature = String.format("%s|%s|%s|%s", transmissionId, transmissionTime, validWebhookId, SSLUtil.crc32(bodyString));

        // Decode the actual signature and update the event object with its data
        String decodedSignature = SSLUtil.decodeTransmissionSignature(actualEncodedSignature);
        String[] arrayDecodedSignature = decodedSignature.split("\\|"); // Split by | char, because the decoded string should look like this: <transmissionId>|<timeStamp>|<webhookId>|<crc32>
        header.setWebhookId(arrayDecodedSignature[2]);
        header.setCrc32(arrayDecodedSignature[3]);

        // Validate the encoded signature.
        // If we are in sandbox mode, we are done with validation here,
        // because the next part will always fail if this event is a mock, sandbox event.
        // For more information see: https://developer.paypal.com/docs/api-basics/notifications/webhooks/notification-messages/
        if (isSandboxMode) {
            event.setValid(true);
            return;
        }

        boolean isSigValid = SSLUtil.validateTransmissionSignature(clientCerts, authAlgo, actualEncodedSignature, expectedDecodedSignature);
        if (isSigValid){
            // Lastly check if the webhook ids match
            if (!header.getWebhookId().equals(event.getValidWebhookId()))
                throw new WebHookValidationException("The events provided webhook id("+header.getWebhookId()+") does not match the valid id("+event.getValidWebhookId()+")!");

            event.setValid(true);
        }
        else
            throw new WebHookValidationException("Transmission signature is not valid! Expected: '"+expectedDecodedSignature+"' Provided: '"+decodedSignature+"'");
    }

    /**
     * Formats all of the WebHooks information to a String and returns it.
     * @param webHookEvent
     */
    public String getWebhookAsString(WebhookEvent webHookEvent) {
        Objects.requireNonNull(webHookEvent);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("Information for object: "+webHookEvent +System.lineSeparator());

        // Add your info
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("VALID-webhook-id: "+webHookEvent.getValidWebhookId() +System.lineSeparator());
        stringBuilder.append("VALID-webhook-types: "+webHookEvent.getValidTypesList().toString() +System.lineSeparator());

        // Add header info
        stringBuilder.append(System.lineSeparator());
        WebhookEventHeader header = webHookEvent.getHeader();
        stringBuilder.append("header stuff: "+System.lineSeparator());
        stringBuilder.append("webhook-id: "+header.getWebhookId() +System.lineSeparator());
        stringBuilder.append("transmission-id: "+header.getTransmissionId() +System.lineSeparator());
        stringBuilder.append("timestamp: "+header.getTimestamp() +System.lineSeparator());
        stringBuilder.append("transmission-sig: "+header.getTransmissionSignature() +System.lineSeparator());
        stringBuilder.append("auth-algo: "+header.getAuthAlgorithm() +System.lineSeparator());
        stringBuilder.append("cert-url: "+header.getCertUrl() +System.lineSeparator());
        stringBuilder.append("crc32: "+header.getCrc32() +System.lineSeparator());

        // Add the json body in a pretty format
        stringBuilder.append(System.lineSeparator());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(webHookEvent.getBodyString());
        stringBuilder.append("body-string: "+webHookEvent.getBodyString() +System.lineSeparator());
        stringBuilder.append("body: "+jsonOutput +System.lineSeparator());

        return stringBuilder.toString();
    }

    public boolean isSandboxMode() {
        return isSandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        isSandboxMode = sandboxMode;
    }
}
