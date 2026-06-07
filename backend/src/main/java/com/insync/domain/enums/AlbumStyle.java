package com.insync.domain.enums;

public enum AlbumStyle {

    // sort by exif timestamp
    CHRONOLOGICAL,
    // group by dominant color palette using K-means clustering
    COLOR_PALETTE,
    // prioritize image with detected faces
    PORTRAIT_GALLERY,
    // alternate high-complexity and minimal images
    HIGH_CONTRAST
}
