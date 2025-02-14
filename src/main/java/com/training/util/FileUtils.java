package com.training.util;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {
    
    public static MultipartFile convertFileToMultipartFile(File file, String originalFilename) throws IOException {
        return new MockMultipartFile(
            originalFilename,
            originalFilename,
            "application/pdf",
            Files.readAllBytes(file.toPath())
        );
    }
}
