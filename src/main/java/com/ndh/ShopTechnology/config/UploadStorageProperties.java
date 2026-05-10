package com.ndh.ShopTechnology.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gốc vật lý thư mục upload: mọi file lưu dưới {@code {resolvedRoot}/{yyMMdd}/filename}.
 * Đường dẫn lưu DB ({@code DocumentEntity#filePath}) vẫn dùng dạng {@code /uploads/{yyMMdd}/...} để hiển thị.
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class UploadStorageProperties {

    /**
     * Đường dẫn thư mục chứa file; có thể tuyệt đối hoặc tương đối {@code user.dir}.
     */
    private String root = "uploads";

    private Path resolvedRoot;

    @PostConstruct
    void normalizePath() {
        Path p = Paths.get(root.trim());
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir", ".")).resolve(p).normalize();
        } else {
            p = p.normalize();
        }
        this.resolvedRoot = p;
    }

    public Path getResolvedRoot() {
        if (resolvedRoot == null) {
            normalizePath();
        }
        return resolvedRoot;
    }
}
