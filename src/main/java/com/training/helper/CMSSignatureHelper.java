package com.training.helper;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.pdfbox.io.IOUtils;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for CMS signature operations.
 */
@Slf4j
public class CMSSignatureHelper {

    static {
        // Ensure BouncyCastle provider is added
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Creates a CMS signature for the given content.
     * 
     * @param content The content to sign
     * @param privateKey The private key to sign with
     * @param certificateChain The certificate chain
     * @return The encoded CMS signature
     * @throws IOException If there's an issue with the content
     */
    public static byte[] createCMSSignature(byte[] content, PrivateKey privateKey, 
                                           Certificate[] certificateChain) throws IOException {
        try {
            log.info("Creating CMS signature");
            
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) certificateChain[0];
            
            // Add the signing certificate and chain
            try {
                gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
            } catch (Exception e) {
                log.error("Error adding certificates to CMS generator: {}", e.getMessage(), e);
                throw new IOException("Error adding certificates: " + e.getMessage(), e);
            }
            
            // Add signer info with signed attributes
            JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256withRSA");
            contentSignerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

            // Create signer info with default attributes (includes messageDigest and signingTime)
            try {
                gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build())
                        .build(contentSignerBuilder.build(privateKey), cert));
            } catch (Exception e) {
                log.error("Error adding signer info to CMS generator: {}", e.getMessage(), e);
                throw new IOException("Error adding signer info: " + e.getMessage(), e);
            }

            // Create signed data
            CMSProcessableByteArray msg = new CMSProcessableByteArray(content);
            CMSSignedData signedData = gen.generate(msg, false);
            
            log.info("CMS signature created successfully");
            return signedData.getEncoded();
        } catch (CMSException e) {
            log.error("Error creating CMS signature: {}", e.getMessage(), e);
            throw new IOException("Error creating CMS signature: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculates the estimated size of a signature based on the certificate chain and private key.
     * 
     * @param pdfFile The PDF file to sign
     * @param chain The certificate chain
     * @param privateKey The private key
     * @return The estimated signature size
     */
    public static int calculateSignatureSize(MultipartFile pdfFile, Certificate[] chain, PrivateKey privateKey) {
        try {
            log.info("Calculating signature size");
            
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            JcaCertStore certs;
            try {
                certs = new JcaCertStore(Arrays.asList(chain));
                gen.addCertificates(certs);
            } catch (Exception e) {
                log.error("Error adding certificates to CMS generator: {}", e.getMessage(), e);
                return 8192; // Default size if certificate handling fails
            }

            ContentSigner signer;
            try {
                signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(privateKey);
            } catch (OperatorCreationException e) {
                log.error("Error creating content signer: {}", e.getMessage(), e);
                throw new IOException("Error creating content signer: " + e.getMessage(), e);
            }

            try {
                gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build())
                        .build(signer, (X509Certificate) chain[0]));
            } catch (Exception e) {
                log.error("Error adding signer info to CMS generator: {}", e.getMessage(), e);
                return 8192; // Default size if signer info handling fails
            }

            byte[] signatureBytes = gen.generate(
                new CMSProcessableByteArray(pdfFile.getBytes()), true).getEncoded();

            int estimatedSize = signatureBytes.length;
            log.info("Estimated signature size: {} bytes", estimatedSize);

            return estimatedSize;
        } catch (Exception e) {
            log.error("Failed to calculate signature size: {}", e.getMessage(), e);
            return 8192; // Default size if calculation fails
        }
    }
    
    /**
     * Creates a CMS signature for external signing.
     * 
     * @param content The content to sign
     * @param privateKey The private key to sign with
     * @param certificateChain The certificate chain
     * @return The encoded CMS signature
     * @throws IOException If there's an issue with the content
     */
    public static byte[] createExternalCMSSignature(byte[] content, PrivateKey privateKey, 
                                                  Certificate[] certificateChain) throws IOException {
        try {
            log.info("Creating external CMS signature");
            
            // Create CMS signature
            Store certStore;
            try {
                certStore = new JcaCertStore(Arrays.asList(certificateChain));
            } catch (Exception e) {
                log.error("Error creating certificate store: {}", e.getMessage(), e);
                throw new IOException("Error creating certificate store: " + e.getMessage(), e);
            }
            
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            ContentSigner signer;
            try {
                signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(privateKey);
            } catch (OperatorCreationException e) {
                log.error("Error creating content signer: {}", e.getMessage(), e);
                throw new IOException("Error creating content signer: " + e.getMessage(), e);
            }
            
            try {
                gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build())
                        .build(signer, (X509Certificate) certificateChain[0]));
            } catch (Exception e) {
                log.error("Error adding signer info to CMS generator: {}", e.getMessage(), e);
                throw new IOException("Error adding signer info: " + e.getMessage(), e);
            }
            
            try {
                gen.addCertificates(certStore);
            } catch (Exception e) {
                log.error("Error adding certificates to CMS generator: {}", e.getMessage(), e);
                throw new IOException("Error adding certificates: " + e.getMessage(), e);
            }
            
            CMSProcessableByteArray msg = new CMSProcessableByteArray(content);
            CMSSignedData signedData = gen.generate(msg, false);
            
            log.info("External CMS signature created successfully");
            return signedData.getEncoded();
        } catch (CMSException e) {
            log.error("Error creating external CMS signature: {}", e.getMessage(), e);
            throw new IOException("Error creating external CMS signature: " + e.getMessage(), e);
        }
    }
}
