package com.insync.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlotLayout (
        UUID photoId, int position, double cropX, double cropY,
        double cropWidth, double cropHeight, boolean isFullPage
) {
    public static SlotLayout of(UUID photoId, int position) {
        return SlotLayout.builder().photoId(photoId).position(position).cropX(0.0).cropY(0.0).cropWidth(1.0).cropHeight(1.0).isFullPage(false).build();
    }

    public static SlotLayout fullPage(UUID photoId) {
        return SlotLayout.builder().photoId(photoId).position(0).cropX(0.0).cropY(0.0).cropWidth(1.0).cropHeight(1.0).isFullPage(true).build();
    }
}