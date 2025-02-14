package com.training.service;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Service
public class PdfSigningService {
    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] signPdf(MultipartFile pdfFile, String certificatePath, String certificatePassword,
                         String certificateAlias, String signatureName, String signatureLocation) throws IOException, CertificateException {
        // Load the keystore (certificate)
        KeyStore keystore;
        try {
            keystore = KeyStore.getInstance("PKCS12");
            try (InputStream certStream = new FileInputStream(certificatePath)) {
                keystore.load(certStream, certificatePassword.toCharArray());
            }

            // Get the private key and certificate chain
            privateKey = (PrivateKey) keystore.getKey(certificateAlias, certificatePassword.toCharArray());
            certificateChain = keystore.getCertificateChain(certificateAlias);

            if (privateKey == null || certificateChain == null || certificateChain.length == 0) {
                throw new CertificateException("Failed to load private key or certificate chain");
            }

        } catch (Exception e) {
            throw new CertificateException("Error loading certificate: " + e.getMessage(), e);
        }

        // Create PDFBox signature interface
        SignatureInterface signatureInterface = content -> {
            try {
                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                X509Certificate cert = (X509Certificate) certificateChain[0];
                
                // Add the signing certificate and chain
                gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
                
                // Verify certificate validity
                cert.checkValidity();

                // Add signer info with signed attributes
                JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256withRSA");
                contentSignerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

                // Create signer info with default attributes (includes messageDigest and signingTime)
                gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build())
                        .build(contentSignerBuilder.build(privateKey), cert));

                // Read content into memory using buffered stream
                BufferedInputStream bufferedContent = new BufferedInputStream(content);
                byte[] contentBytes = IOUtils.toByteArray(bufferedContent);
                
                // Create signed data using buffered content
                CMSProcessableByteArray msg = new CMSProcessableByteArray(contentBytes);
                CMSSignedData signedData = gen.generate(msg, true);
                
                return signedData.getEncoded();
            } catch (Exception e) {
                throw new RuntimeException("Error creating signature: " + e.getMessage(), e);
            }
        };

        PDDocument document = null;
        SignatureOptions signatureOptions = null;
        try {
            // Load the PDF document
            document = PDDocument.load(pdfFile.getInputStream());

            // Create signature dictionary
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signatureName);
            signature.setLocation(signatureLocation);
            signature.setReason("Document digitally signed");
            Calendar signingTime = Calendar.getInstance();
            signature.setSignDate(signingTime);

            // Create signature options with preferred size
            signatureOptions = new SignatureOptions();
            // Reserve enough space for the signature (increased size for safety)
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 4);

            // Register signature dictionary and sign interface
            document.addSignature(signature, signatureInterface, signatureOptions);

            // Save the signed document to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.saveIncremental(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IOException("Error processing PDF: " + e.getMessage(), e);
        } finally {
            if (signatureOptions != null) {
                try {
                    signatureOptions.close();
                } catch (IOException e) {
                    // Log but don't throw as we're in finally
                }
            }
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // Log but don't throw as we're in finally
                }
            }
        }
    }
}
