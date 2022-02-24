package com.osiris.payhook.paypal;

import com.osiris.payhook.PayPalWebHookEventValidator;

import java.util.Map;

/**
 * Contains the {@link PaypalWebhookEvent}s headers
 * essential information for its validation. <br>
 * Can be created thorough {@link PayPalWebHookEventValidator#parseAndGetHeader(Map)}.
 */
public class PaypalWebhookEventHeader {
    private final String transmissionId;
    private final String timestamp;
    private final String transmissionSignature;
    private final String authAlgorithm;
    private final String certUrl;
    private String webhookId;
    private String crc32;

    /**
     * Contains the {@link PaypalWebhookEvent}s headers
     * essential information for its validation.
     * Can be created thorough {@link PayPalWebHookEventValidator#parseAndGetHeader(Map)}.
     */
    public PaypalWebhookEventHeader(String transmissionId, String timestamp, String transmissionSignature, String authAlgorithm, String certUrl) {
        this.transmissionId = transmissionId;
        this.timestamp = timestamp;
        this.webhookId = null; // Gets set once validation was run
        this.crc32 = null; // Gets set once validation was run
        this.transmissionSignature = transmissionSignature;
        this.authAlgorithm = authAlgorithm;
        this.certUrl = certUrl;
    }

    /**
     * The unique ID of the HTTP transmission.
     * Contained in PAYPAL-TRANSMISSION-ID header of the notification message.
     */
    public String getTransmissionId() {
        return transmissionId;
    }

    /**
     * The date and time when the HTTP message was transmitted.
     * Contained in PAYPAL-TRANSMISSION-TIME header of the notification message.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * The ID of the webhook resource for the destination URL to which PayPal delivers the event notification. <br>
     * NOTE 1: SINCE THE WEBHOOK-ID IS INSIDE THE ENCODED TRANSMISSION-SIGNATURE, THIS RETURNS NULL
     * UNLESS YOU SUCCESSFULLY EXECUTED {@link PayPalWebHookEventValidator#validateWebhookEvent(PaypalWebhookEvent)} ONCE BEFORE! <br>
     * NOTE 2: IF YOU HAVE SANDBOX-MODE ENABLED THIS WILL ALWAYS RETURN NULL, EVEN IF YOU ALREADY
     * EXECUTED {@link PayPalWebHookEventValidator#validateWebhookEvent(PaypalWebhookEvent)} ONCE BEFORE.
     */
    public String getWebhookId() {
        return webhookId;
    }

    /**
     * See {@link PaypalWebhookEventHeader#getWebhookId()} for details.
     */
    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    /**
     * The Cyclic Redundancy Check (CRC32) checksum for the body of the HTTP payload. <br>
     * NOTE 1: SINCE THE CRC32 IS INSIDE THE ENCODED TRANSMISSION-SIGNATURE, THIS RETURNS NULL
     * UNLESS YOU SUCCESSFULLY EXECUTED {@link PayPalWebHookEventValidator#validateWebhookEvent(PaypalWebhookEvent)} ONCE BEFORE! <br>
     * NOTE 2: IF YOU HAVE SANDBOX-MODE ENABLED THIS WILL ALWAYS RETURN NULL, EVEN IF YOU ALREADY
     * EXECUTED {@link PayPalWebHookEventValidator#validateWebhookEvent(PaypalWebhookEvent)} ONCE BEFORE. <br>
     */
    public String getCrc32() {
        return crc32;
    }

    /**
     * See {@link PaypalWebhookEventHeader#getCrc32()} for details.
     */
    public void setCrc32(String crc32) {
        this.crc32 = crc32;
    }

    /**
     * The PayPal-generated asymmetric signature.
     */
    public String getTransmissionSignature() {
        return transmissionSignature;
    }

    /**
     * The algorithm that PayPal used to generate the signature and that you can use to verify the signature.
     */
    public String getAuthAlgorithm() {
        return authAlgorithm;
    }

    /**
     * The X509 public key certificate.
     * Download the certificate from this URL and use it to verify the signature.
     */
    public String getCertUrl() {
        return certUrl;
    }
}
