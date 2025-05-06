package com.training.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.training.helper.SignatureAppearanceHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for signing PDFs with Adobe Acrobat compatible signatures.
 */
@Service
@Slf4j
public class AcroLoadedSigningService {
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Signs a PDF document with a digital signature using layered appearance.
     * 
     * @param pdfFile The PDF file to sign
     * @param certificatePath Path to the certificate file
     * @param certificatePassword Password for the certificate
     * @param certificateAlias Alias of the certificate in the keystore
     * @param signatureName Name of the signer
     * @param signatureLocation Location of the signer
     * @param reason Reason for signing
     * @param page The page number to place the signature (1-based)
     * @param x X-coordinate for the signature
     * @param y Y-coordinate for the signature
     * @param width Width of the signature field
     * @param height Height of the signature field
     * @return The signed PDF as a byte array
     * @throws IOException If there's an issue with the PDF
     * @throws CertificateException If there's an issue with the certificate
     */
    public byte[] signPdfWithLayers(MultipartFile pdfFile, 
                                  String certificatePath, 
                                  String certificatePassword,
                                  String certificateAlias, 
                                  String signatureName, 
                                  String signatureLocation,
                                  String reason,
                                  int page,
                                  float x, float y, 
                                  float width, float height) 
                                  throws IOException, CertificateException {
        
        log.info("Signing PDF with layered appearance");
        
        // Load the keystore and get the private key and certificate chain
        KeyStore keystore;
        PrivateKey privateKey;
        Certificate[] certificateChain;
        
        try {
            keystore = KeyStore.getInstance("PKCS12");
        } catch (Exception e) {
            throw new CertificateException("Error creating keystore: " + e.getMessage(), e);
        }
        
        try (InputStream certStream = new FileInputStream(certificatePath)) {
            keystore.load(certStream, certificatePassword.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(certificateAlias, certificatePassword.toCharArray());
            certificateChain = keystore.getCertificateChain(certificateAlias);
            
            if (privateKey == null || certificateChain == null || certificateChain.length == 0) {
                throw new CertificateException("Failed to load private key or certificate chain");
            }
        } catch (Exception e) {
            throw new CertificateException("Error loading certificate: " + e.getMessage(), e);
        }
        
        // Create signature interface for the actual signing process
        SignatureInterface signatureInterface = content -> {
            try {
                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                X509Certificate cert = (X509Certificate) certificateChain[0];
                
                // Add the signing certificate and chain
                gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
                
                // Verify certificate validity
                cert.checkValidity();
                
                // Add signer info
                JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256withRSA");
                contentSignerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
                
                gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build())
                        .build(contentSignerBuilder.build(privateKey), cert));
                
                // Create signed data
                byte[] contentBytes = IOUtils.toByteArray(content);
                CMSProcessableByteArray msg = new CMSProcessableByteArray(contentBytes);
                CMSSignedData signedData = gen.generate(msg, true);
                
                return signedData.getEncoded();
            } catch (Exception e) {
                throw new IOException("Error creating signature: " + e.getMessage(), e);
            }
        };
        
        // Load the PDF document
        byte[] pdfBytes = IOUtils.toByteArray(pdfFile.getInputStream());
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            // Check if the page number is valid
            if (page < 1 || page > document.getNumberOfPages()) {
                throw new IOException("Invalid page number: " + page);
            }
            
            // Create signature dictionary
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signatureName);
            signature.setLocation(signatureLocation);
            signature.setReason(reason);
            signature.setSignDate(Calendar.getInstance());
            
            PDRectangle signatureRect = new PDRectangle(x, y, width, height);
            
            // Create layered signature appearance
            PDAppearanceDictionary appearance = createLayeredAppearance(
                document, signatureRect, signatureName, signatureLocation, 
                signature.getSignDate(), reason);
            
            // Set up signature options
            SignatureOptions signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(7503370);
            // Create a temporary file for the visual signature
            File tempFile = File.createTempFile("signature", ".pdf");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                // Create a temporary document with the appearance
                PDDocument tempDoc = new PDDocument();
                PDPage tempPage = new PDPage(new PDRectangle(signatureRect.getWidth(), signatureRect.getHeight()));
                tempDoc.addPage(tempPage);
                
                // Add the appearance to the temp document
                PDAcroForm acroForm = new PDAcroForm(tempDoc);
                tempDoc.getDocumentCatalog().setAcroForm(acroForm);
                
                // Create a signature field with the appearance
                PDSignatureField signatureField = new PDSignatureField(acroForm);
                
                // Explicitly set the field type to SIG
                signatureField.getCOSObject().setItem(COSName.FT, COSName.SIG);
                
                // Get the widget annotation and set its properties
                PDAnnotationWidget widget = signatureField.getWidgets().get(0);
                
                // Explicitly set the annotation type
                widget.getCOSObject().setItem(COSName.TYPE, COSName.ANNOT);
                widget.getCOSObject().setItem(COSName.SUBTYPE, COSName.WIDGET);
                
                // Set appearance and rectangle
                widget.setAppearance(appearance);
                widget.setRectangle(new PDRectangle(0, 0, signatureRect.getWidth(), signatureRect.getHeight()));
                
                // Add the widget to the page
                tempPage.getAnnotations().add(widget);
                
                // Add the field to the form
                acroForm.getFields().add(signatureField);
                
                log.info("Created temporary signature template with all required objects");
                
                // Save the temp document
                tempDoc.save(fos);
                tempDoc.close();
            }
            
            // Set the visual signature from the temp file
            signatureOptions.setVisualSignature(tempFile);
            signatureOptions.setPage(page - 1); // Convert to 0-based index
            
            // Add signature to document
            document.addSignature(signature, signatureInterface, signatureOptions);
            
            // Save with incremental update to preserve existing signatures
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.saveIncremental(baos);
            
            log.info("PDF signed successfully with layered appearance");
            return baos.toByteArray();
        }
    }
    
    /**
     * Creates a layered signature appearance following Adobe Acrobat's conventions.
     */
    private PDAppearanceDictionary createLayeredAppearance(
            PDDocument document, PDRectangle rect, 
            String signatureName, String signatureLocation, 
            Calendar signingTime, String reason) throws IOException {
        
        log.info("Creating layered signature appearance with DESCRIPTION rendering mode");
        
        // Create the appearance dictionary
        PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();
        
        // Create a dictionary to hold all the named appearances (n0, n1, n2, n3, n4)
        PDResources resources = new PDResources();
        
        // Create each layer as a separate appearance stream
        PDAppearanceStream n0Stream = SignatureAppearanceHelper.createLayerN0(document, rect);
        PDAppearanceStream n1Stream = SignatureAppearanceHelper.createLayerN1(document, rect);
        PDAppearanceStream n2Stream = SignatureAppearanceHelper.createLayerN2(document, rect, signatureName, 
                                                            signatureLocation, signingTime);
        PDAppearanceStream n3Stream = SignatureAppearanceHelper.createLayerN3(document, rect);
        PDAppearanceStream n4Stream = SignatureAppearanceHelper.createLayerN4(document, rect);
        
        // Add all layers to the resources
        resources.put(COSName.getPDFName("n0"), n0Stream);
        resources.put(COSName.getPDFName("n1"), n1Stream);
        resources.put(COSName.getPDFName("n2"), n2Stream);
        resources.put(COSName.getPDFName("n3"), n3Stream);
        resources.put(COSName.getPDFName("n4"), n4Stream);
        
        // Create the FRM layer that combines all layers
        PDAppearanceStream frm = new PDAppearanceStream(document);
        frm.setResources(resources);
        frm.setBBox(rect);
        
        // Create content stream for FRM that references all other layers
        try (PDPageContentStream cs = new PDPageContentStream(document, frm)) {
            // Add n0 layer
            cs.saveGraphicsState();
            cs.drawForm((PDFormXObject) resources.getXObject(COSName.getPDFName("n0")));
            cs.restoreGraphicsState();
            
            // Add n1 layer
            cs.saveGraphicsState();
            cs.drawForm((PDFormXObject) resources.getXObject(COSName.getPDFName("n1")));
            cs.restoreGraphicsState();
            
            // Add n2 layer
            cs.saveGraphicsState();
            cs.drawForm((PDFormXObject) resources.getXObject(COSName.getPDFName("n2")));
            cs.restoreGraphicsState();
            
            // Add n3 layer
            cs.saveGraphicsState();
            cs.drawForm((PDFormXObject) resources.getXObject(COSName.getPDFName("n3")));
            cs.restoreGraphicsState();
            
            // Add n4 layer
            cs.saveGraphicsState();
            cs.drawForm((PDFormXObject) resources.getXObject(COSName.getPDFName("n4")));
            cs.restoreGraphicsState();
        }
        
        // Add the FRM to the resources
        resources.put(COSName.getPDFName("FRM"), frm);
        
        // Set the normal appearance to use the FRM
        appearanceDict.setNormalAppearance(frm);
        
        log.info("Layered signature appearance created successfully");
        return appearanceDict;
    }
    
    /**
     * Simplified version of the signing method with default values.
     */
    public byte[] signPdf(MultipartFile pdfFile, String certificatePath, String certificatePassword,
                         String certificateAlias, String signatureName, String signatureLocation) 
                         throws IOException, CertificateException {
        
        // Default values
        String reason = "Document digitally signed";
        int page = 1;
        float x = 0;
        float y = 0;
        float width = 300;
        float height = 100;
        
        return signPdfWithLayers(pdfFile, certificatePath, certificatePassword, certificateAlias,
                              signatureName, signatureLocation, reason,
                              page, x, y, width, height);
    }
}
