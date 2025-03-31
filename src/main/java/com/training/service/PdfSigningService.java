package com.training.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class PdfSigningService {

    public byte[] signPdf(MultipartFile pdfFile, String certificatePath, String certificatePassword,
                         String certificateAlias, String signatureName, String signatureLocation) throws IOException, CertificateException {
        try {
            // Load the PDF document
            byte[] pdfBytes = pdfFile.getBytes();
            DSSDocument document = new InMemoryDocument(pdfBytes);
            
            // Load the PKCS12 keystore
            Pkcs12SignatureToken token = new Pkcs12SignatureToken(
                certificatePath, 
                new PasswordProtection(certificatePassword.toCharArray())
            );
            
            // Get the private key entry
            List<DSSPrivateKeyEntry> keys = token.getKeys();
            DSSPrivateKeyEntry privateKey = keys.get(0); // Use the first key
            
            // Configure signature parameters
            PAdESSignatureParameters parameters = new PAdESSignatureParameters();
            // Use PAdES_BASELINE_B for basic validation
            parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LTA);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
            parameters.setSigningCertificate(privateKey.getCertificate());
            parameters.setCertificateChain(privateKey.getCertificateChain());
            
            // Enable signature validation visualization in Adobe Reader
            parameters.setContentHintsDescription("This document has been digitally signed");
            parameters.setContentIdentifierPrefix("DSS-");
            parameters.setContentIdentifierSuffix("-Signed");
            
            // Set signature location and reason
            parameters.setLocation(signatureLocation);
            parameters.setReason("Document digitally signed");
            
            // Format the current date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDate = dateFormat.format(new Date());
            
            // Create a visible signature appearance
            SignatureImageParameters imageParameters = new SignatureImageParameters();
            
            // Set the signature position (bottom left corner)
            SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
            fieldParameters.setOriginX(50);
            fieldParameters.setOriginY(50);
            fieldParameters.setWidth(200);
            fieldParameters.setHeight(70);
            imageParameters.setFieldParameters(fieldParameters);
            
            // Configure text in signature
            SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
            textParameters.setText("Digitally signed by: " + signatureName + "\n" +
                                  "Location: " + signatureLocation + "\n" +
                                  "Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            imageParameters.setTextParameters(textParameters);
            
            // Apply the image parameters to the signature parameters
            parameters.setImageParameters(imageParameters);
            
            // Create PAdES service
            CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
            PAdESService service = new PAdESService(commonCertificateVerifier);
            OnlineTSPSource tspSource = new OnlineTSPSource();
            service.setTspSource(tspSource);
            // Get the data to be signed
            ToBeSigned dataToSign = service.getDataToSign(document, parameters);
            
            // Sign the data
            SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
            
            // Apply the signature to the document
            DSSDocument signedDocument = service.signDocument(document, parameters, signatureValue);
            
            // Convert the signed document to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            signedDocument.writeTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error signing PDF: " + e.getMessage(), e);
        }
    }
}
