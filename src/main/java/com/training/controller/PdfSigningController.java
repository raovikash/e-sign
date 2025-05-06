package com.training.controller;

import com.training.enums.SignatureAppearanceType;
import com.training.service.AcroLoadedSigningService;
import com.training.service.ContentWithLayersService;
import com.training.service.PdfSigningService;
import com.training.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.nullable;

import java.io.File;

@RestController
@RequestMapping("/api/pdf")
@Slf4j
public class PdfSigningController {

    @Autowired
    private PdfSigningService pdfSigningService;

    @Autowired
    private AcroLoadedSigningService acroLoadedSigningService;

    @Autowired
    private ContentWithLayersService contentWithLayersService;

    @GetMapping("/layeredPage")
    public ResponseEntity<byte[]> addContentToPdfLayers(@RequestParam("n1") String n1Text, 
    @RequestParam("n2") String n2Text, @RequestParam("n3") String n3Text, 
    @RequestParam("n4") String n4Text) throws Exception{
        String tempPdfPath = "/Users/vikash.yadav/Documents/e-sign/e-sign/ESIGN_DOCUMENT_BRTPELN00014AD9_LOAN_AGREEMENT_COPY.pdf";
        File pdfFile = new File(tempPdfPath);
        MultipartFile multipartFile = FileUtils.convertFileToMultipartFile(pdfFile, "ESIGN_DOCUMENT_BRTPELN00014AD9_LOAN_AGREEMENT_COPY.pdf");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "signed_generated.pdf");
        byte[] signedPdf = contentWithLayersService.addLayers(multipartFile, n1Text, n2Text, n3Text, n4Text);
        return ResponseEntity.ok()
          .headers(headers)
          .body(signedPdf);
    }

    @GetMapping("/sign")
    public ResponseEntity<byte[]> generateAndSignPdf(@RequestParam("signatureAppearanceType") SignatureAppearanceType signatureAppearanceType) {
        try {
            final String certificatePath = "/Users/vikash.yadav/Documents/e-sign/e-sign/test_certificate.p12";
            final String certificatePassword = "password123";
            final String certificateAlias = "testcert";
            final String signatureName = "Test Signer";
            final String signatureLocation = "City";

            // Generate a sample PDF
            String tempPdfPath = "/Users/vikash.yadav/Documents/e-sign/e-sign/ESIGN_DOCUMENT_BRTPELN00014AD9_LOAN_AGREEMENT.pdf";
                        
            // Read the generated PDF
            File pdfFile = new File(tempPdfPath);
            MultipartFile multipartFile = FileUtils.convertFileToMultipartFile(pdfFile, "ESIGN_DOCUMENT_BRTPELN00014AD9_LOAN_AGREEMENT_DSC.pdf");
            // Sign the PDF
            byte[] signedPdf = null;
            switch (signatureAppearanceType) {
                case OLD: 
                    signedPdf = acroLoadedSigningService.signPdf(
                        multipartFile,
                        certificatePath,
                        certificatePassword,
                        certificateAlias,
                        signatureName,
                        signatureLocation
                    );
                    break;
                case NEW: 
                    signedPdf = pdfSigningService.signPdf(
                        multipartFile,
                        certificatePath,
                        certificatePassword,
                        certificateAlias,
                        signatureName,
                        signatureLocation
                    );
                    break;
                default:
                    // Default to NEW if no valid type is provided
                    signedPdf = pdfSigningService.signPdf(
                        multipartFile,
                        certificatePath,
                        certificatePassword,
                        certificateAlias,
                        signatureName,
                        signatureLocation
                    );
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "signed_generated.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(signedPdf);
        } catch (Exception e) {
            log.error("Error generating and signing PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
