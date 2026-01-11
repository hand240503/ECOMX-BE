package com.ndh.ShopTechnology.utils;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.time.LocalDate;

public class FileUtils {
    private static final String UPLOADS_FOLDER = "uploads";

    public static DocumentEntity storeFile(MultipartFile file) throws IOException {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = FilenameUtils.getExtension(filename);
        String uniqueFilename = UUID.randomUUID().toString() + "_" + System.nanoTime() + "." + extension;

        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear()).substring(2);
        String month = String.format("%02d", today.getMonthValue());
        String day = String.format("%02d", today.getDayOfMonth());

        String datePath = year + month + day;
        Path dateDir = Paths.get(UPLOADS_FOLDER, datePath);

        if (!Files.exists(dateDir)) {
            Files.createDirectories(dateDir);
        }

        Path destination = dateDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);


        return DocumentEntity.builder()
                .fileName(uniqueFilename)
                .fileSize(String.valueOf(file.getSize()))
                .filePath(getRelativePath(datePath, uniqueFilename))
                .type(0) // Set appropriate type as needed
                .build();
    }

    private static String getRelativePath(String datePath, String uniqueFilename) {
        String relativePath = Paths.get(UPLOADS_FOLDER, datePath, uniqueFilename).toString();
        return "/" + relativePath.replace("\\", "/");
    }
}
