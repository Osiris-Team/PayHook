package com.osiris.payhook;

public class WebhookEventHeader {
    private String transmissionId;
    private String timestamp;
    private String webhookId;
    private String crc32;
    private String transmissionSignature;
    private String authAlgorithm;
    private String certUrl;

    public WebhookEventHeader(String transmissionId, String timestamp, String transmissionSignature, String authAlgorithm, String certUrl) {
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
     * IMPORTANT: SINCE THE WEBHOOK ID IS INSIDE THE ENCRYPTED TRANSMISSION SIGNATURE, THIS RETURNS NULL
     * UNLESS YOU SUCCESSFULLY EXECUTED {@link PayHook#validateWebHookEvent(WebhookEvent)} ONCE BEFORE!
     * The ID of the webhook resource for the destination URL to which PayPal delivers the event notification.
     */
    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    /**
     * IMPORTANT: SINCE THE CRC32 IS INSIDE THE ENCRYPTED TRANSMISSION SIGNATURE, THIS RETURNS NULL
     * UNLESS YOU SUCCESSFULLY EXECUTED {@link PayHook#validateWebHookEvent(WebhookEvent)} ONCE BEFORE!
     * The Cyclic Redundancy Check (CRC32) checksum for the body of the HTTP payload.
     */
    public String getCrc32() {
        return crc32;
    }

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
