package com.ndh.ShopTechnology.utils;

import com.ndh.ShopTechnology.constants.DocumentKind;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class FileUtils {

    public static final String PUBLIC_UPLOAD_SEGMENT = "uploads";

    private FileUtils() {
    }

    public static DocumentEntity storeFile(MultipartFile file, Path uploadRoot) throws IOException {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = FilenameUtils.getExtension(filename);
        String uniqueFilename = UUID.randomUUID().toString() + "_" + System.nanoTime()
                + (extension == null || extension.isEmpty() ? "" : "." + extension);

        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear()).substring(2);
        String month = String.format("%02d", today.getMonthValue());
        String day = String.format("%02d", today.getDayOfMonth());

        String datePath = year + month + day;
        Path dateDir = uploadRoot.resolve(datePath);
        if (!Files.exists(dateDir)) {
            Files.createDirectories(dateDir);
        }

        Path destination = dateDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return DocumentEntity.builder()
                .fileName(uniqueFilename)
                .fileSize(String.valueOf(file.getSize()))
                .filePath(relativePublicPath(datePath, uniqueFilename))
                .type(DocumentKind.resolve(file))
                .main(false)
                .build();
    }

    private static String relativePublicPath(String datePath, String uniqueFilename) {
        return "/" + PUBLIC_UPLOAD_SEGMENT + "/" + datePath + "/" + uniqueFilename;
    }
}
