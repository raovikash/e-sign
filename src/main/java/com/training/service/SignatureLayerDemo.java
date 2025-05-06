package com.training.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.graphics.form.*;
import org.apache.pdfbox.pdmodel.graphics.state.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.apache.pdfbox.pdmodel.font.*;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

public class SignatureLayerDemo {
    public void signPdf(MultipartFile pdfFile, String certificatePath, String certificatePassword,
                        String certificateAlias, String signatureName, String signatureLocation) throws Exception {

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream keyStream = new FileInputStream(certificatePath)) {
            keystore.load(keyStream, certificatePassword.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) keystore.getKey(certificateAlias, certificatePassword.toCharArray());
        Certificate[] certificateChain = keystore.getCertificateChain(certificateAlias);

        File signedPdf = new File("signed_output.pdf");
        try (PDDocument document = Loader.loadPDF(IOUtils.toByteArray(pdfFile.getInputStream()))) {
            PDPage page = document.getPage(0);
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signatureName);
            signature.setLocation(signatureLocation);
            signature.setSignDate(Calendar.getInstance());

            document.addSignature(signature);

            ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(new FileOutputStream(signedPdf));

            // create CMS signature
            List<Certificate> certList = Arrays.asList(certificateChain);
            Store certStore = new JcaCertStore(certList);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);

            gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                            .build(sha1Signer, (X509Certificate) certificateChain[0]));

            gen.addCertificates(certStore);
            CMSProcessableByteArray msg = new CMSProcessableByteArray(externalSigning.getContent().readAllBytes());
            CMSSignedData signedData = gen.generate(msg, false);
            byte[] cmsSignature = signedData.getEncoded();

            externalSigning.setSignature(cmsSignature);
        }
    }

    // private static PDFormXObject createLayer(PDDocument document, PDRectangle rect, String label, String text, int r, int g, int b) throws IOException {
    //     PDFormXObject form = new PDFormXObject(document);
    //     form.setResources(new PDResources());
    //     form.setBBox(new PDRectangle(0, 0, rect.getWidth(), rect.getHeight()));

    //     try (PDPageContentStream cs = new PDPageContentStream(document, form)) {
    //         cs.setNonStrokingColor(r, g, b);
    //         cs.addRect(0, 0, rect.getWidth(), rect.getHeight());
    //         cs.fill();

    //         cs.beginText();
    //         cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
    //         cs.newLineAtOffset(10, rect.getHeight() - 20);
    //         cs.showText(label + ": " + text);
    //         cs.endText();
    //     }

    //     return form;
    // }
}
