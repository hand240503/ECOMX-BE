package com.ndh.ShopTechnology.constants;

import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

public final class DocumentKind {

    private DocumentKind() {
    }

    public static final int IMAGE = 1;

    public static final int VIDEO = 2;

    public static final int DOCUMENT = 3;

    public static int resolve(MultipartFile file) {
        if (file == null) {
            return DOCUMENT;
        }
        String ct = file.getContentType();
        if (ct != null) {
            String lower = ct.toLowerCase();
            if (lower.startsWith("image/")) {
                return IMAGE;
            }
            if (lower.startsWith("video/")) {
                return VIDEO;
            }
            if (lower.startsWith("application/pdf")
                    || lower.startsWith("application/msword")
                    || lower.startsWith("application/vnd.openxmlformats-officedocument")
                    || lower.startsWith("application/vnd.ms-excel")
                    || lower.startsWith("text/plain")) {
                return DOCUMENT;
            }
        }
        String name = file.getOriginalFilename();
        String ext = name != null ? FilenameUtils.getExtension(name) : null;
        if (ext == null || ext.isEmpty()) {
            return DOCUMENT;
        }
        ext = ext.toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "avif" -> IMAGE;
            case "mp4", "webm", "mov", "avi", "mkv", "m4v" -> VIDEO;
            default -> DOCUMENT;
        };
    }
}
