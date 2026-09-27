package com.insync.service.storage;


import com.insync.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores files on the local filesystem under the configured base path.
 * Swap this bean for an S3 implementation when moving to production
 * — nothing outside this class needs to change (vision.md §1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageConfig storageConfig;

    @Override
    public String storeOriginal(MultipartFile file) throws IOException {
        validateContentType(file);
        String key = generateKey(file.getContentType());
        Path destination = storageConfig.resolveOriginal(key);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored original: {}", destination);
        return key;
    }

    @Override
    public String storeProcessed(byte[] data, String filename) throws IOException {
        Path destination = storageConfig.resolveProcessed(filename);
        Files.write(destination, data);
        log.debug("Stored processed: {}", destination);
        return filename;
    }

    @Override
    public String storeThumbnail(byte[] data, String filename) throws IOException {
        Path destination = storageConfig.resolveThumbnail(filename);
        Files.write(destination, data);
        log.debug("Stored thumbnail: {}", destination);
        return filename;
    }

    @Override
    public Path resolveOriginal(String storageKey) {
        return storageConfig.resolveOriginal(storageKey);
    }

    @Override
    public Path resolveProcessed(String storageKey) {
        return storageConfig.resolveProcessed(storageKey);
    }

    @Override
    public Path resolveThumbnail(String storageKey) {
        return storageConfig.resolveThumbnail(storageKey);
    }

    @Override
    public void deleteAll(String originalKey, String processedKey, String thumbnailKey) throws IOException {
        deleteIfPresent(storageConfig.resolveOriginal(originalKey));
        if (processedKey != null) {
            deleteIfPresent(storageConfig.resolveProcessed(processedKey));
        }
        if (thumbnailKey != null) {
            deleteIfPresent(storageConfig.resolveThumbnail(thumbnailKey));
        }
    }

    // helpers
    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !storageConfig.getAllowedTypes().contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType + ". Allowed: " + storageConfig.getAllowedTypes());
        }
    }

    private String generateKey(String contentType) {
        String ext = switch(contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
        return UUID.randomUUID() + "." + ext;
    }

    private void deleteIfPresent(Path path) throws IOException {
        boolean deleted = Files.deleteIfExists(path);
        if (deleted) {
            log.debug("Deleted file: {}", path);
        }
    }
}
