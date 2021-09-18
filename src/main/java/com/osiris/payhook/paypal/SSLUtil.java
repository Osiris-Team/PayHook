package com.osiris.payhook.paypal;

import com.osiris.payhook.exceptions.WebHookValidationException;
import com.osiris.payhook.paypal.codec.binary.Base64;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Class SSLUtil
 */
public abstract class SSLUtil {

    /**
     * KeyManagerFactory used for {@link SSLContext} {@link KeyManager}
     */
    private static final KeyManagerFactory KMF;

    /**
     * Private {@link Map} used for caching {@link KeyStore}s
     */
    private static final Map<String, KeyStore> STOREMAP;

    static {
        try {

            // Initialize KeyManagerFactory and local KeyStore cache
            KMF = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            STOREMAP = new HashMap<String, KeyStore>();
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Performs Certificate Chain Validation on provided certificates. The method verifies if the client certificates provided are generated from root certificates
     * trusted by application.
     *
     * @param clientCerts Collection of X509Certificates provided in request
     * @param trustCerts  Collection of X509Certificates trusted by application
     * @param authType    Auth Type for Certificate
     * @return true if client and server are chained together, false otherwise
     */
    public static boolean validateCertificateChain(Collection<X509Certificate> clientCerts, Collection<X509Certificate> trustCerts, String authType) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, WebHookValidationException {
        TrustManager[] trustManagers;
        X509Certificate[] clientChain;
        clientChain = clientCerts.toArray(new X509Certificate[0]);
        List<X509Certificate> list = Arrays.asList(clientChain);
        clientChain = list.toArray(new X509Certificate[0]);

        // Create a Keystore and load the Root CA Cert
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, "".toCharArray());

        // Iterate through each certificate and add to keystore
        int i = 0;
        for (Iterator<X509Certificate> payPalCertificate = trustCerts.iterator(); payPalCertificate.hasNext(); ) {
            X509Certificate x509Certificate = payPalCertificate.next();
            keyStore.setCertificateEntry("paypalCert" + i, x509Certificate);
            i++;
        }

        // Create TrustManager
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        // For Each TrustManager of type X509
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager pkixTrustManager = (X509TrustManager) trustManager;
                // Check the trust manager if server is trusted
                pkixTrustManager.checkClientTrusted(clientChain, (authType == null || authType == "") ? "RSA" : authType);
                // Checks that the certificate is currently valid. It is if the current date and time are within the validity period given in the certificate.
                for (X509Certificate cert : clientChain) {
                    cert.checkValidity();
                    // Check for CN name matching
                    String dn = cert.getSubjectX500Principal().getName();
                    String[] tokens = dn.split(",");
                    boolean hasPaypalCn = false;

                    for (String token : tokens) {
                        if (token.startsWith("CN=messageverificationcerts") && token.endsWith(".paypal.com")) {
                            hasPaypalCn = true;
                        }
                    }

                    if (!hasPaypalCn) {
                        throw new WebHookValidationException("CN of client certificate does not match with trusted CN");
                    }
                }
                // If everything looks good, return true
                return true;
            }
        }

        return false;
    }

    /**
     * Generate Collection of Certificate from Input Stream
     *
     * @param stream InputStream of Certificate data
     * @return Collection<X509Certificate>
     */
    @SuppressWarnings("unchecked")
    public static Collection<X509Certificate> getCertificateFromStream(InputStream stream) throws CertificateException {
        Objects.requireNonNull(stream);
        Collection<X509Certificate> certs = null;
        CertificateFactory cf = CertificateFactory.getInstance("X.509"); // Create a Certificate Factory
        certs = (Collection<X509Certificate>) cf.generateCertificates(stream); // Read the Trust Certs
        return certs;
    }

    /**
     * Generates a CRC 32 Value of String passed
     *
     * @param data
     * @return long crc32 value of input. -1 if string is null
     * @throws RuntimeException if UTF-8 is not a supported character set
     */
    public static long crc32(String data) {
        if (data == null) {
            return -1;
        }

        // get bytes from string
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        Checksum checksum = new CRC32();
        // update the current checksum with the specified array of bytes
        checksum.update(bytes, 0, bytes.length);
        // get the current checksum value
        return checksum.getValue();
    }

    /**
     * Validates Webhook Signature validation based on https://developer.paypal.com/docs/integration/direct/rest-webhooks-overview/#event-signature
     * Returns true if signature is valid
     *
     * @param clientCerts              Client Certificates
     * @param algo                     Algorithm used for signature creation by server
     * @param actualEncodedSignature   Paypal-Transmission-Sig header value passed by server
     * @param expectedDecodedSignature Signature generated by formatting data with CRC32 value of request body
     * @return Returns true if signature is valid
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws InvalidKeyException
     */
    public static boolean validateTransmissionSignature(Collection<X509Certificate> clientCerts, String algo,
                                                        String actualEncodedSignature, String expectedDecodedSignature) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // Get the signatureAlgorithm from the PAYPAL-AUTH-ALGO HTTP header
        Signature signatureAlgorithm = Signature.getInstance(algo);
        // Get the certData from the URL provided in the HTTP headers and cache it
        X509Certificate[] clientChain = clientCerts.toArray(new X509Certificate[0]);
        signatureAlgorithm.initVerify(clientChain[0].getPublicKey());
        signatureAlgorithm.update(expectedDecodedSignature.getBytes());
        // Actual signature is base 64 encoded and available in the HTTP headers
        byte[] actualSignature = Base64.decodeBase64(actualEncodedSignature.getBytes());
        return signatureAlgorithm.verify(actualSignature);
    }

    public static String decodeTransmissionSignature(String encodedSignature) {
        return new String(Base64.decodeBase64(encodedSignature.getBytes()), StandardCharsets.UTF_8);
    }
}
