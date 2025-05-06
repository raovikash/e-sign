package com.training.helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for certificate operations including loading and validation.
 */
@Slf4j
public class CertificateHelper {

    /**
     * Loads a certificate from a PKCS12 keystore file.
     * 
     * @param certificatePath Path to the certificate file
     * @param certificatePassword Password for the certificate
     * @param certificateAlias Alias of the certificate in the keystore
     * @return An array containing the private key and certificate chain
     * @throws CertificateException If there's an issue with the certificate
     * @throws IOException If there's an issue reading the certificate file
     */
    public static Object[] loadCertificate(String certificatePath, String certificatePassword, 
                                          String certificateAlias) throws CertificateException, IOException {
        log.info("Loading certificate from: {}", certificatePath);
        
        KeyStore keystore;
        try {
            keystore = KeyStore.getInstance("PKCS12");
            try (InputStream certStream = new FileInputStream(certificatePath)) {
                keystore.load(certStream, certificatePassword.toCharArray());
            }

            // Get the private key and certificate chain
            PrivateKey privateKey = (PrivateKey) keystore.getKey(certificateAlias, certificatePassword.toCharArray());
            Certificate[] certificateChain = keystore.getCertificateChain(certificateAlias);

            if (privateKey == null || certificateChain == null || certificateChain.length == 0) {
                throw new CertificateException("Failed to load private key or certificate chain");
            }
            
            log.info("Certificate loaded successfully");
            return new Object[] { privateKey, certificateChain };

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            log.error("Error loading certificate: {}", e.getMessage(), e);
            throw new CertificateException("Error loading certificate: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates a certificate to ensure it's not expired.
     * 
     * @param certificate The certificate to validate
     * @throws CertificateException If the certificate is invalid or expired
     */
    public static void validateCertificate(Certificate certificate) throws CertificateException {
        if (certificate instanceof X509Certificate) {
            X509Certificate x509Cert = (X509Certificate) certificate;
            try {
                x509Cert.checkValidity();
                log.info("Certificate is valid");
            } catch (CertificateException e) {
                log.error("Certificate validation failed: {}", e.getMessage(), e);
                throw new CertificateException("Certificate validation failed: " + e.getMessage(), e);
            }
        } else {
            log.warn("Certificate is not an X509Certificate, skipping validation");
        }
    }
}
