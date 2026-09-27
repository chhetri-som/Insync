package com.insync.web.dto.response;

import com.insync.domain.enums.PhotoProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        UUID albumId,
        String originalStorageKey,
        String processedStorageKey, // null, unless logic developed
        String thumbnailStorageKey, // null, for now ig
        PhotoProcessingStatus processingStatus,
        int uploadOrder,
        LocalDateTime createdAt
) {
}
