package com.osiris.payhook.paypal;

public final class Constants {

    private Constants() {}

    // PayPal webhook transmission ID HTTP request header
    public static final String PAYPAL_HEADER_TRANSMISSION_ID = "PAYPAL-TRANSMISSION-ID";

    // PayPal webhook transmission time HTTP request header
    public static final String PAYPAL_HEADER_TRANSMISSION_TIME = "PAYPAL-TRANSMISSION-TIME";

    // PayPal webhook transmission signature HTTP request header
    public static final String PAYPAL_HEADER_TRANSMISSION_SIG = "PAYPAL-TRANSMISSION-SIG";

    // PayPal webhook certificate URL HTTP request header
    public static final String PAYPAL_HEADER_CERT_URL = "PAYPAL-CERT-URL";

    // PayPal webhook authentication algorithm HTTP request header
    public static final String PAYPAL_HEADER_AUTH_ALGO = "PAYPAL-AUTH-ALGO";

    // Trust Certificate Location to be used to validate webhook certificates
    public static final String PAYPAL_TRUST_CERT_URL = "webhook.trustCert";

    // Default Trust Certificate that comes packaged with SDK.
    public static final String PAYPAL_TRUST_DEFAULT_CERT = "DigiCertSHA2ExtendedValidationServerCA.crt";

}
