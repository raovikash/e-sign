package com.training.controller;

import com.training.service.PdfSigningService;
import com.training.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/pdf")
@Slf4j
public class PdfSigningController {

    @Autowired
    private PdfSigningService pdfSigningService;

    @GetMapping("/sign")
    public ResponseEntity<byte[]> generateAndSignPdf() {
        try {
            final String certificatePath = "/Users/vikash.yadav/Documents/training/training-demo/test_certificate.p12";
            final String certificatePassword = "password123";
            final String certificateAlias = "testcert";
            final String signatureName = "Test Signer";
            final String signatureLocation = "City";

            // Generate a sample PDF
            String tempPdfPath = "/Users/vikash.yadav/Documents/e-sign/e-sign/Rent_Agreement_Aadhar_Esigned.pdf";
                        
            // Read the generated PDF
            File pdfFile = new File(tempPdfPath);
            MultipartFile multipartFile = FileUtils.convertFileToMultipartFile(pdfFile, "Rent_Agreement_Aadhar_Esigned.pdf");

            // Sign the PDF
            byte[] signedPdf = pdfSigningService.signPdf(
                multipartFile,
                certificatePath,
                certificatePassword,
                certificateAlias,
                signatureName,
                signatureLocation
            );

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
