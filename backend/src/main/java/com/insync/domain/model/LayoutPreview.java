package com.insync.domain.model;

import com.insync.domain.enums.AlbumStyle;

import java.util.List;
import java.util.UUID;

public record LayoutPreview (
        AlbumStyle style, List<PageLayout> pages, UUID coverPhotoId, int photoCount, int pageCount
) {
    public LayoutPreview {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("A layout preview must have at least one page");
        }
        pages = List.copyOf(pages);
    }
}
