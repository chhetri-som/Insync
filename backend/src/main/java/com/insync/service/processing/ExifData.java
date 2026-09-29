package com.insync.service.processing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExifData(
        LocalDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static ExifData empty() {
        return new ExifData(null, null, null);
    }

    public boolean hasGps() {
        return latitude != null && longitude != null;
    }
}
