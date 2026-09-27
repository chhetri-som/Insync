package com.insync.service.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

/*
 * Abstracts file I/O so LocalStorageService can be swapped for S3
 * without touching any business logic (see vision.md §1).
 *
 * Callers always work with a "storage key" — a short relative filename
 * like "a3f2c1b0.jpg". The StorageService knows which on-disk subdirectory
 * (originals / processed / thumbnails) to use for each operation.
 */
public interface StorageService {

    String storeOriginal(MultipartFile file) throws IOException;
    String storeProcessed(byte[] data, String filename) throws IOException;
    String storeThumbnail(byte[] data, String filename) throws IOException;
    Path resolveOriginal(String storageKey);
    Path resolveProcessed(String storageKey);
    Path resolveThumbnail(String storageKey);
    void deleteAll(String originalKey, String processedKey, String thumbnailKey) throws IOException;
}
