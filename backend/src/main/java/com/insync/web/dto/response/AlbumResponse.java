package com.insync.web.dto.response;

import com.insync.domain.enums.AlbumStatus;
import com.insync.domain.enums.AlbumStyle;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlbumResponse(
        UUID id,
        String title,
        AlbumStyle style,
        AlbumStatus status,
        long photoCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
