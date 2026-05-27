package com.ndh.ShopTechnology.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class UploadStorageProperties {

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
