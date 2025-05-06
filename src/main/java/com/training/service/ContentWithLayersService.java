package com.training.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

@Service
public class ContentWithLayersService {
    
    /**
     * Adds text to different layers of a PDF document.
     * 
     * @param multipartFile The PDF file to add layers to
     * @param n1Text Text to add to layer n1
     * @param n2Text Text to add to layer n2
     * @param n3Text Text to add to layer n3
     * @param n4Text Text to add to layer n4
     * @return The modified PDF as a byte array
     * @throws IOException If there's an error processing the PDF
     */
    public byte[] addLayers(MultipartFile multipartFile, String n1Text, String n2Text, String n3Text, String n4Text) throws IOException {
        byte[] pdfBytes = IOUtils.toByteArray(multipartFile.getInputStream());
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            // Get or create OCG properties
            PDOptionalContentProperties ocProperties = document.getDocumentCatalog().getOCProperties();
            if (ocProperties == null) {
                ocProperties = new PDOptionalContentProperties();
                document.getDocumentCatalog().setOCProperties(ocProperties);
            }
            
            // Create layers (OCGs)
            PDOptionalContentGroup n1Layer = createOrGetLayer(ocProperties, "n1");
            PDOptionalContentGroup n2Layer = createOrGetLayer(ocProperties, "n2");
            PDOptionalContentGroup n3Layer = createOrGetLayer(ocProperties, "n3");
            PDOptionalContentGroup n4Layer = createOrGetLayer(ocProperties, "n4");
            
            // Get the first page
            PDPage firstPage = document.getPage(0);
            
            // Add text to each layer
            addTextToLayer(document, firstPage, n1Layer, n1Text, 50, 750);
            addTextToLayer(document, firstPage, n2Layer, n2Text, 50, 730);
            addTextToLayer(document, firstPage, n3Layer, n3Text, 50, 710);
            addTextToLayer(document, firstPage, n4Layer, n4Text, 50, 690);
            
            // Save the document to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    /**
     * Creates a new layer or gets an existing one with the given name.
     * 
     * @param ocProperties The document's optional content properties
     * @param layerName The name of the layer
     * @return The layer (OCG)
     */
    private PDOptionalContentGroup createOrGetLayer(PDOptionalContentProperties ocProperties, String layerName) {
        // Check if layer already exists
        PDOptionalContentGroup layer = null;
        // In PDFBox 3.0.5, we need to use a different approach to get OCGs
        Iterable<PDOptionalContentGroup> ocgs = ocProperties.getOptionalContentGroups();
        for (PDOptionalContentGroup ocg : ocgs) {
            if (layerName.equals(ocg.getName())) {
                layer = ocg;
                break;
            }
        }
        if (layer == null) {
            // Create new layer
            layer = new PDOptionalContentGroup(layerName);
            ocProperties.addGroup(layer);
        }
        return layer;
    }
    
    /**
     * Adds text to a specific layer at the given position.
     * 
     * @param document The PDF document
     * @param page The page to add text to
     * @param layer The layer to add text to
     * @param text The text to add
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @throws IOException If there's an error adding text
     */
    private void addTextToLayer(PDDocument document, PDPage page, PDOptionalContentGroup layer, 
                               String text, float x, float y) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            // Set the layer for this content
            contentStream.beginMarkedContent(COSName.OC, layer);
            
            // Set font and add text
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
            
            // End the marked content
            contentStream.endMarkedContent();
        }
    }
}
