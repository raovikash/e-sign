package com.training.helper;

import java.awt.Color;
import java.io.IOException;
import java.util.Calendar;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for creating and managing signature appearances
 * with proper layer structure for Adobe Acrobat compatibility.
 */
@Slf4j
public class SignatureAppearanceHelper {

    /**
     * Creates the normal appearance dictionary for the signature with multiple layers
     * following Adobe Acrobat's layer structure requirements
     */
    public static PDAppearanceDictionary createSignatureAppearance(PDDocument document, PDRectangle rect, 
                                                     String signatureName, String signatureLocation, 
                                                     Calendar signingTime) throws IOException {
        log.info("Creating signature appearance with multiple layers");
        
        // Create the appearance dictionary
        PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();
        
        // Create a dictionary to hold all the named appearances (n0, n1, n2, n3, n4)
        COSDictionary normalAppearances = new COSDictionary();
        
        // Create each layer as a separate appearance stream
        PDAppearanceStream n0Stream = createLayerN0(document, rect);
        PDAppearanceStream n1Stream = createLayerN1(document, rect);
        PDAppearanceStream n2Stream = createLayerN2(document, rect, signatureName, signatureLocation, signingTime);
        PDAppearanceStream n3Stream = createLayerN3(document, rect);
        PDAppearanceStream n4Stream = createLayerN4(document, rect);
        
        // Add all layers to the normal appearances dictionary with their proper names
        normalAppearances.setItem(COSName.getPDFName("n0"), n0Stream);
        normalAppearances.setItem(COSName.getPDFName("n1"), n1Stream);
        normalAppearances.setItem(COSName.getPDFName("n2"), n2Stream);
        normalAppearances.setItem(COSName.getPDFName("n3"), n3Stream);
        normalAppearances.setItem(COSName.getPDFName("n4"), n4Stream);
        
        // Set the normal appearance to use the dictionary of named appearances
        appearanceDict.getCOSObject().setItem(COSName.N, normalAppearances);
        
        log.info("Signature appearance created successfully");
        return appearanceDict;
    }
    
    /**
     * Layer 0 (n0): A blank base layer with border
     */
    private static PDAppearanceStream createLayerN0(PDDocument document, PDRectangle rect) throws IOException {
        PDAppearanceStream n0Stream = new PDAppearanceStream(document);
        n0Stream.setResources(new PDResources());
        n0Stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        
        // Create content stream for n0
        try (PDPageContentStream n0Content = new PDPageContentStream(document, n0Stream)) {
            // Just set a white background
            n0Content.setNonStrokingColor(Color.WHITE);
            n0Content.addRect(0, 0, rect.getWidth(), rect.getHeight());
            n0Content.fill();
            
            // Draw a border around the signature
            n0Content.setStrokingColor(Color.BLACK);
            n0Content.setLineWidth(1);
            n0Content.addRect(1, 1, rect.getWidth() - 2, rect.getHeight() - 2);
            n0Content.stroke();
        }
        
        return n0Stream;
    }
    
    /**
     * Layer 1 (n1): Contains a question mark (for older Acrobat versions)
     */
    private static PDAppearanceStream createLayerN1(PDDocument document, PDRectangle rect) throws IOException {
        PDAppearanceStream n1Stream = new PDAppearanceStream(document);
        n1Stream.setResources(new PDResources());
        n1Stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        
        // Create content stream for n1
        try (PDPageContentStream n1Content = new PDPageContentStream(document, n1Stream)) {
            // Draw a question mark in the corner
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            n1Content.beginText();
            n1Content.setFont(font, 24);
            n1Content.setNonStrokingColor(Color.GRAY);
            n1Content.newLineAtOffset(rect.getWidth() - 30, 10);
            n1Content.showText("?");
            n1Content.endText();
        }
        
        return n1Stream;
    }
    
    /**
     * Layer 2 (n2): Contains the signature information
     */
    private static PDAppearanceStream createLayerN2(PDDocument document, PDRectangle rect, 
                                           String signatureName, String signatureLocation, 
                                           Calendar signingTime) throws IOException {
        PDAppearanceStream n2Stream = new PDAppearanceStream(document);
        n2Stream.setResources(new PDResources());
        n2Stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        
        // Create content stream for n2
        try (PDPageContentStream n2Content = new PDPageContentStream(document, n2Stream)) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            n2Content.beginText();
            n2Content.setFont(font, 9);
            n2Content.setNonStrokingColor(Color.BLACK);
            n2Content.newLineAtOffset(10, rect.getHeight() - 20);
            n2Content.showText("Digitally signed by: " + signatureName);
            n2Content.newLineAtOffset(0, -12);
            n2Content.showText("Location: " + signatureLocation);
            n2Content.newLineAtOffset(0, -12);
            n2Content.showText("Date: " + signingTime.getTime().toString());
            n2Content.endText();
        }
        
        return n2Stream;
    }
    
    /**
     * Layer 3 (n3): Another blank layer
     */
    private static PDAppearanceStream createLayerN3(PDDocument document, PDRectangle rect) throws IOException {
        PDAppearanceStream n3Stream = new PDAppearanceStream(document);
        n3Stream.setResources(new PDResources());
        n3Stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        
        // No content needed for n3, it's a blank layer
        
        return n3Stream;
    }
    
    /**
     * Layer 4 (n4): Contains "Signature Not Verified" text
     */
    private static PDAppearanceStream createLayerN4(PDDocument document, PDRectangle rect) throws IOException {
        PDAppearanceStream n4Stream = new PDAppearanceStream(document);
        n4Stream.setResources(new PDResources());
        n4Stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        
        // Create content stream for n4
        try (PDPageContentStream n4Content = new PDPageContentStream(document, n4Stream)) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            n4Content.beginText();
            n4Content.setFont(font, 10);
            n4Content.setNonStrokingColor(Color.RED);
            n4Content.newLineAtOffset(10, 10);
            n4Content.showText("Signature Not Verified");
            n4Content.endText();
        }
        
        return n4Stream;
    }
}
