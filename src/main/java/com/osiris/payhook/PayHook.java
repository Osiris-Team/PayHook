package com.osiris.payhook;

import com.google.gson.*;
import com.osiris.payhook.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.ParseBodyException;
import com.osiris.payhook.exceptions.ParseHeaderException;
import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.Constants;
import com.osiris.payhook.paypal.SSLUtil;
import com.osiris.payhook.paypal.codec.binary.Base64;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

/**
 * Contains methods for validating
 * WebHook events/notifications and some
 * utility methods.
 */
public class PayHook {
    private final PayHookValidationType validationType;
    private boolean isSandboxMode = false;
    private boolean isWarnIfSandboxModeIsEnabled = true;
    private String clientId;
    private String clientSecret;
    private String encodedCredentials;

    /**
     * Creates a new {@link PayHook} object with {@link PayHookValidationType#ONLINE}. <br>
     * See {@link #PayHook(PayHookValidationType, String, String)} for details.
     */
    public PayHook(String clientId, String clientSecret){
        this(PayHookValidationType.ONLINE, clientId, clientSecret);
    }

    /**
     * @param validationType the type of validation method to use for validating {@link WebhookEvent}s. <br>
     *                       ONLINE will use the online PayPal REST-API to validate the {@link WebhookEvent} (recommended).<br>
     *                       OFFLINE will use the offline validation method. More details here: {@link #validateWebhookEvent(WebhookEvent)}.
     * @param clientId       the PayPal client id. Only needed when {@link PayHookValidationType#ONLINE} is selected, otherwise can be null.
     * @param clientSecret   the PayPal client secret. Only needed when {@link PayHookValidationType#ONLINE} is selected, otherwise can be null.
     */
    public PayHook(PayHookValidationType validationType, String clientId, String clientSecret) {
        this.validationType = validationType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        if (validationType.equals(PayHookValidationType.ONLINE)) {
            System.err.println("PayPal credentials must be provided to use PayHook in ONLINE mode!");
            Objects.requireNonNull(clientId);
            Objects.requireNonNull(clientSecret);
            this.encodedCredentials = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Creates and returns a new {@link WebhookEvent} object that can get validated.
     *
     * @param validId      the valid Webhook id.
     * @param validTypes   the valid Webhook-Event types
     * @param headersAsMap the requests headers as map.
     * @param bodyAsString the requests body as string.
     * @throws ParseHeaderException
     * @throws ParseBodyException
     */
    public WebhookEvent createWebhookEvent(String validId, List<String> validTypes, Map<String, String> headersAsMap, String bodyAsString) throws ParseHeaderException, ParseBodyException {
        return new WebhookEvent(
                validId,
                validTypes,
                parseAndGetHeader(headersAsMap),
                parseAndGetBody(bodyAsString));
    }

    /**
     * Creates a new {@link WebhookEvent} object from the provided details and validates it.
     *
     * @param validId      the valid Webhook id.
     * @param validTypes   the valid Webhook-Event types
     * @param headersAsMap the requests headers as map.
     * @param bodyAsString the requests body as string.
     * @return the validated {@link WebhookEvent}.
     * @throws ParseHeaderException
     * @throws ParseBodyException
     */
    public WebhookEvent createAndValidateWebhookEvent(String validId, List<String> validTypes, Map<String, String> headersAsMap, String bodyAsString) throws ParseHeaderException, ParseBodyException, WebHookValidationException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, HttpErrorException {
        WebhookEvent event = new WebhookEvent(
                validId,
                validTypes,
                parseAndGetHeader(headersAsMap),
                parseAndGetBody(bodyAsString));
        validateWebhookEvent(event);
        return event;
    }

    /**
     * Creates a new {@link WebhookEvent} object from the provided details, validates it and returns the result.
     *
     * @param validId      the valid Webhook id.
     * @param validTypes   the valid Webhook-Event types
     * @param headersAsMap the requests headers as map.
     * @param bodyAsString the requests body as string.
     * @return true if the {@link WebhookEvent} is valid.
     * @throws ParseHeaderException
     * @throws ParseBodyException
     */
    public boolean isWebhookEventValid(String validId, List<String> validTypes, Map<String, String> headersAsMap, String bodyAsString) throws ParseHeaderException, ParseBodyException, WebHookValidationException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, HttpErrorException {
        WebhookEvent event = new WebhookEvent(
                validId,
                validTypes,
                parseAndGetHeader(headersAsMap),
                parseAndGetBody(bodyAsString));
        validateWebhookEvent(event);
        return event.isValid();
    }

    /**
     * Parses the provided header {@link Map}
     * into a {@link WebhookEventHeader} object and returns it.
     */
    public WebhookEventHeader parseAndGetHeader(Map<String, String> headerAsMap) throws ParseHeaderException {
        // Check if all keys we need exist
        String transmissionId = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_ID);
        String timestamp = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_TIME);
        String transmissionSignature = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_TRANSMISSION_SIG);
        String certUrl = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_CERT_URL);
        String authAlgorithm = checkKeyAndGetValue(headerAsMap, Constants.PAYPAL_HEADER_AUTH_ALGO);

        // Note that the webhook id and crc32 get set after the validation was run
        return new WebhookEventHeader(transmissionId, timestamp, transmissionSignature, authAlgorithm, certUrl);
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

    /**
     * Validate the provided {@link WebhookEvent}.<br>
     * Throws an {@link Exception} if the validation fails. <br>
     * Performed checks for {@link PayHookValidationType#ONLINE}: <br>
     * Not known since validation is handled by the PayPal Rest-API. <br>
     * Performed checks for {@link PayHookValidationType#OFFLINE}: <br>
     * <ul>
     *   <li>Is the name/type valid?</li>
     *   <li>Are the certificates valid?</li>
     *   <li>Is the data/transmission-signature valid? (not in sandbox-mode)</li>
     *   <li>Do the webhook ids match? (not in sandbox-mode)</li>
     * </ul>
     * Note: {@link WebhookEventHeader#getWebhookId()} and {@link WebhookEventHeader#getCrc32()} return null,
     * if you have sandbox-mode <u>enabled</u> since the transmission-signature is <u>not</u> decoded in sandbox-mode.
     *
     * @param event The {@link WebhookEvent} to validate.
     * @throws WebHookValidationException <b style='color:red' >IMPORTANT: MESSAGE MAY CONTAIN SENSITIVE INFORMATION!</b>
     */
    public void validateWebhookEvent(WebhookEvent event) throws WebHookValidationException, ParseBodyException, IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, HttpErrorException {

        if (isSandboxMode && isWarnIfSandboxModeIsEnabled)
            System.err.println("[PAYHOOK] NOTE THAT SANDBOX-MODE IS ENABLED!");

        WebhookEventHeader header = event.getHeader();

        if (validationType.equals(PayHookValidationType.ONLINE)) {

            String json = "{\n" +
                    "  \"transmission_id\": \"" + header.getTransmissionId() + "\",\n" +
                    "  \"transmission_time\": \"" + header.getTimestamp() + "\",\n" +
                    "  \"cert_url\": \"" + header.getCertUrl() + "\",\n" +
                    "  \"auth_algo\": \"" + header.getAuthAlgorithm() + "\",\n" +
                    "  \"transmission_sig\": \"" + header.getTransmissionSignature() + "\",\n" +
                    "  \"webhook_id\": \"" + header.getWebhookId() + "\",\n" +
                    "  \"webhook_event\": " + event.getBodyString() + "\n" +
                    "}";

            new PayPalJsonUtils().postJsonAndGetResponse("/v1/notifications/verify-webhook-signature", json, encodedCredentials);

        } else {

            // Check if the webhook types match
            List<String> validEventTypes = event.getValidTypesList();

            // event_type can be either an json array or a normal field. Do stuff accordingly.
            JsonElement elementEventType = event.getBody().get("event_type");
            if (elementEventType == null)
                elementEventType = event.getBody().get("event_types"); // Check for event_types
            if (elementEventType == null)
                throw new ParseBodyException("Failed to find key 'event_type' or 'event_types' in the provided json body."); // if the element is still null

            if (elementEventType.isJsonArray()) {
                // This means we have multiple event_type objects in the array
                JsonArray arrayEventType = elementEventType.getAsJsonArray();
                for (JsonElement singleElementEventType :
                        arrayEventType) {
                    JsonObject o = singleElementEventType.getAsJsonObject();
                    if (!validEventTypes.contains(o.get("name").getAsString()))
                        throw new WebHookValidationException("No valid type(" + o.get("name") + ") found in the valid types list: " + validEventTypes);
                }
            } else {
                // This means we only have one event_type in the json and not an array.
                String webHookType = event.getBody().get("event_type").getAsString();
                if (!validEventTypes.contains(webHookType))
                    throw new WebHookValidationException("No valid type(" + webHookType + ") found in the valid types list: " + validEventTypes);
            }

            // Load certs
            String clientCertificateLocation = event.getHeader().getCertUrl();
            String trustCertificateLocation = Constants.PAYPAL_TRUST_DEFAULT_CERT;
            Collection<X509Certificate> clientCerts = SSLUtil.getCertificateFromStream(new BufferedInputStream(new URL(clientCertificateLocation).openStream()));
            Collection<X509Certificate> trustCerts = SSLUtil.getCertificateFromStream(PayHook.class.getClassLoader().getResourceAsStream(trustCertificateLocation));

            // Check the chain
            SSLUtil.validateCertificateChain(clientCerts, trustCerts, "RSA");

            // Validate the encoded signature.
            // Note:
            // If we are in sandbox mode, we are done with validation here,
            // because the next part will always fail if this event is a mock, sandbox event.
            // For more information see: https://developer.paypal.com/docs/api-basics/notifications/webhooks/notification-messages/
            if (isSandboxMode) {
                event.setValid(true);
                return;
            }

            // Construct expected signature
            String validWebhookId = event.getValidWebhookId();
            String actualEncodedSignature = header.getTransmissionSignature();
            String authAlgo = header.getAuthAlgorithm();
            String transmissionId = header.getTransmissionId();
            String transmissionTime = header.getTimestamp();
            String bodyString = event.getBodyString();
            String expectedDecodedSignature = String.format("%s|%s|%s|%s", transmissionId, transmissionTime, validWebhookId, SSLUtil.crc32(bodyString));

            // Decode the actual signature and update the event object with its data
            String decodedSignature = SSLUtil.decodeTransmissionSignature(actualEncodedSignature);
            String[] arrayDecodedSignature = decodedSignature.split("\\|"); // Split by | char, because the decoded string should look like this: <transmissionId>|<timeStamp>|<webhookId>|<crc32>
            header.setWebhookId(arrayDecodedSignature[2]);
            header.setCrc32(arrayDecodedSignature[3]);

            boolean isSigValid = SSLUtil.validateTransmissionSignature(clientCerts, authAlgo, actualEncodedSignature, expectedDecodedSignature);
            if (isSigValid)
                event.setValid(true);
            else
                throw new WebHookValidationException("Transmission signature is not valid! Expected: '" + expectedDecodedSignature + "' Provided: '" + decodedSignature + "'");

        }
    }

    /**
     * Formats all of the {@link WebhookEvent} information to a {@link String} and returns it.
     */
    public String getWebhookEventAsString(WebhookEvent webHookEvent) {
        Objects.requireNonNull(webHookEvent);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("Information for object: " + webHookEvent + System.lineSeparator());

        // Add your info
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("VALID-webhook-id: " + webHookEvent.getValidWebhookId() + System.lineSeparator());
        stringBuilder.append("VALID-webhook-types: " + webHookEvent.getValidTypesList().toString() + System.lineSeparator());

        // Add header info
        stringBuilder.append(System.lineSeparator());
        WebhookEventHeader header = webHookEvent.getHeader();
        stringBuilder.append("header stuff: " + System.lineSeparator());
        stringBuilder.append("webhook-id: " + header.getWebhookId() + System.lineSeparator());
        stringBuilder.append("transmission-id: " + header.getTransmissionId() + System.lineSeparator());
        stringBuilder.append("timestamp: " + header.getTimestamp() + System.lineSeparator());
        stringBuilder.append("transmission-sig: " + header.getTransmissionSignature() + System.lineSeparator());
        stringBuilder.append("auth-algo: " + header.getAuthAlgorithm() + System.lineSeparator());
        stringBuilder.append("cert-url: " + header.getCertUrl() + System.lineSeparator());
        stringBuilder.append("crc32: " + header.getCrc32() + System.lineSeparator());

        // Add the json body in a pretty format
        stringBuilder.append(System.lineSeparator());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(webHookEvent.getBodyString());
        stringBuilder.append("body-string: " + webHookEvent.getBodyString() + System.lineSeparator());
        stringBuilder.append("body: " + jsonOutput + System.lineSeparator());

        return stringBuilder.toString();
    }

    /**
     * See {@link PayHook#setSandboxMode(boolean)} for details.
     */
    public boolean isSandboxMode() {
        return isSandboxMode;
    }

    /**
     * Enable/Disable the sandbox-mode. <br>
     * Disabled by default. <br>
     * If enabled some validation checks, which only succeed for
     * live applications, wont be done. <br>
     * See {@link PayHook#validateWebhookEvent(WebhookEvent)} for details.
     */
    public void setSandboxMode(boolean sandboxMode) {
        isSandboxMode = sandboxMode;
    }

    /**
     * See {@link PayHook#setWarnIfSandboxModeIsEnabled(boolean)} for details.
     */
    public boolean isWarnIfSandboxModeIsEnabled() {
        return isWarnIfSandboxModeIsEnabled;
    }

    /**
     * If enabled a warning is printed to {@link System#out}
     * every time before performing a validation, stating that the sandbox-mode is enabled. <br>
     * Enabled by default. <br>
     */
    public void setWarnIfSandboxModeIsEnabled(boolean warnIfSandboxModeIsEnabled) {
        isWarnIfSandboxModeIsEnabled = warnIfSandboxModeIsEnabled;
    }

    public PayHookValidationType getValidationType() {
        return validationType;
    }
}
