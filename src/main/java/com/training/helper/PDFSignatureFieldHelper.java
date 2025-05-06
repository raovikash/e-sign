package com.training.helper;

import java.io.IOException;
import java.util.Calendar;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for PDF signature field operations.
 */
@Slf4j
public class PDFSignatureFieldHelper {

    /**
     * Creates a signature field in the document.
     * 
     * @param document The PDF document
     * @param acroForm The AcroForm of the document
     * @param pageIndex The index of the page to add the signature field to (0-based)
     * @param signature The signature to add
     * @param x The x-coordinate of the signature field
     * @param y The y-coordinate of the signature field
     * @param width The width of the signature field
     * @param height The height of the signature field
     * @return The created signature field
     * @throws IOException If there's an issue with the document
     */
    public static PDSignatureField createSignatureField(PDDocument document, PDAcroForm acroForm, 
                                                      int pageIndex, PDSignature signature,
                                                      float x, float y, float width, float height) throws IOException {
        log.info("Creating signature field at position ({}, {}) with size {}x{}", x, y, width, height);
        
        // Create a signature field
        PDSignatureField signatureField = new PDSignatureField(acroForm);
        signatureField.setPartialName("Signature" + System.currentTimeMillis());
        
        // Set the signature to the field
        signatureField.setValue(signature);
        
        // Configure the widget annotation for the signature field
        PDAnnotationWidget widget = signatureField.getWidgets().get(0);
        
        // Set position and size
        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(x);
        rect.setLowerLeftY(y);
        rect.setUpperRightX(x + width);
        rect.setUpperRightY(y + height);
        widget.setRectangle(rect);
        
        // Set the page for the widget
        PDPage page = document.getPage(pageIndex);
        widget.setPage(page);
        
        // Set appearance characteristics for Adobe Reader validation display
        widget.setPrinted(true);
        
        // Add the widget to the page
        page.getAnnotations().add(widget);
        
        // Add the signature field to the form
        acroForm.getFields().add(signatureField);
        
        log.info("Signature field created successfully");
        return signatureField;
    }
    
    /**
     * Sets the appearance of a signature field.
     * 
     * @param signatureField The signature field
     * @param appearanceDict The appearance dictionary
     */
    public static void setSignatureFieldAppearance(PDSignatureField signatureField, 
                                                 PDAppearanceDictionary appearanceDict) {
        log.info("Setting signature field appearance");
        
        // Get the widget annotation for the signature field
        PDAnnotationWidget widget = signatureField.getWidgets().get(0);
        
        // Set the appearance dictionary on the widget
        widget.setAppearance(appearanceDict);
        
        log.info("Signature field appearance set successfully");
    }
    
    /**
     * Creates or gets the AcroForm of a document.
     * 
     * @param document The PDF document
     * @return The AcroForm of the document
     */
    public static PDAcroForm getOrCreateAcroForm(PDDocument document) {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
            log.info("Created new AcroForm");
        } else {
            log.info("Using existing AcroForm");
        }
        
        // Enable appearance generation - critical for Adobe Reader to show validation graphics
        acroForm.setNeedAppearances(true);
        
        return acroForm;
    }
    
    /**
     * Creates a signature dictionary.
     * 
     * @param signatureName The name of the signer
     * @param signatureLocation The location of the signer
     * @param reason The reason for signing
     * @return The created signature dictionary
     */
    public static PDSignature createSignatureDictionary(String signatureName, String signatureLocation, String reason) {
        log.info("Creating signature dictionary");
        
        PDSignature signature = new PDSignature();
        Calendar signingTime = Calendar.getInstance();

        // Configure signature dictionary for Adobe Reader validation display
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName(signatureName);
        signature.setLocation(signatureLocation);
        signature.setReason(reason != null ? reason : "Document digitally signed");
        signature.setSignDate(signingTime);
        
        log.info("Signature dictionary created successfully");
        return signature;
    }
}
