package com.insync.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "insync.storage")
@Getter
@Setter
public class StorageConfig {

    private String basePath;
    private List<String> allowedTypes;

    @PostConstruct
    public void createStorageDirectories() throws IOException {
        Path base = Paths.get(basePath);
        Files.createDirectories(base.resolve("originals"));
        Files.createDirectories(base.resolve("processed"));
        Files.createDirectories(base.resolve("thumbnails"));
    }

    public Path resolveOriginal(String storageKey) {
        return Paths.get(basePath, "originals", storageKey);
    }

    public Path resolveProcessed(String storageKey) {
        return Paths.get(basePath, "processed", storageKey);
    }

    public Path resolveThumbnail(String storageKey) {
        return Paths.get(basePath, "thumbnails", storageKey);
    }
}
