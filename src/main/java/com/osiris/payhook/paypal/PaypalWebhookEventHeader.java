package com.osiris.payhook.paypal;

public class PaypalWebhookEventHeader {
    private final String transmissionId;
    private final String timestamp;
    private final String transmissionSignature;
    private final String authAlgorithm;
    private final String certUrl;
    private String webhookId;

    public PaypalWebhookEventHeader(String transmissionId, String timestamp, String transmissionSignature, String authAlgorithm, String certUrl,
                                    String webhookId) {
        this.transmissionId = transmissionId;
        this.timestamp = timestamp;
        this.transmissionSignature = transmissionSignature;
        this.authAlgorithm = authAlgorithm;
        this.certUrl = certUrl;
        this.webhookId = webhookId;
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
