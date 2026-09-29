package com.insync.service.processing;

import com.insync.domain.model.Photo;
import com.insync.domain.model.PhotoMetadata;
import com.insync.repository.PhotoMetadataRepository;
import com.insync.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataExtractor {
    private static final DateTimeFormatter EXIF_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    private static final List<TagInfo> DATE_TAGS = List.of(
            ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
            ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED,
            TiffTagConstants.TIFF_TAG_DATE_TIME
    );

    private final StorageService storageService;
    private final PhotoMetadataRepository photoMetadataRepository;

    @Transactional
    public PhotoMetadata extract(Photo photo) {
        Path file = storageService.resolveOriginal(photo.getOriginalStorageKey());
        ExifData exif = readExif(file);

        PhotoMetadata metadata = photoMetadataRepository.findByPhoto(photo)
                .orElseGet(() -> PhotoMetadata.builder().photo(photo).build());

        metadata.setTakenAt(exif.takenAt());
        metadata.setLatitude(exif.latitude());
        metadata.setLongitude(exif.longitude());

        log.debug("EXIF for photo {}: takenAt={}, gps = {}", photo.getId(), exif.takenAt(), exif.hasGps());
        return photoMetadataRepository.save(metadata);
    }

    public ExifData readExif(Path file) {
        if (!Files.isRegularFile(file)) {
            log.warn("Cannot read EXIF, file does not exist: {}", file);
            return ExifData.empty();
        }

        try {
            ImageMetadata metadata = Imaging.getMetadata(file.toFile());
            if (!(metadata instanceof JpegImageMetadata jpeg)) {
                return ExifData.empty();
            }
            TiffImageMetadata exif = jpeg.getExif();
            if (exif == null) {
                return ExifData.empty();
            }

            LocalDateTime takenAt = readDate(exif);
            BigDecimal[] gps = readGps(exif);

            return new ExifData(takenAt, gps[0], gps[1]);

        } catch (Exception e) {
            log.warn("EXIF extraction failed for {}: {}", file.getFileName(), e.getMessage());
            return ExifData.empty();
        }
    }

    // helpers

    private LocalDateTime readDate(TiffImageMetadata exif) {
        for (TagInfo tag : DATE_TAGS) {
            try {
                TiffField field = exif.findField(tag);
                if (field == null) {
                    continue;
                }
                LocalDateTime parsed = parseExifDate(field.getStringValue());
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception e) {
                log.debug("Could not read date tag {}: {}", tag.name, e.getMessage());
            }
        }
        return null;
    }

    private BigDecimal[] readGps(TiffImageMetadata exif) {
        try {
            TiffImageMetadata.GpsInfo gps = exif.getGpsInfo();
            if (gps == null) {
                return new BigDecimal[]{null, null};
            }
            double lat = gps.getLatitudeAsDegreesNorth();
            double lon = gps.getLongitudeAsDegreesEast();
            if (!isValidCoordinate(lat, lon) || (lat == 0.0 && lon == 0.0)) {
                return new BigDecimal[] {null, null};
            }
            return new BigDecimal[]{toDecimal(lat), toDecimal(lon)};
        } catch (Exception e) {
            log.debug("Could not read GPS info: {}", e.getMessage());
            return new BigDecimal[] {null, null};
        }
    }

    static LocalDateTime parseExifDate(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replace("\u0000", "").trim();
        if (cleaned.isEmpty() || cleaned.startsWith("0000")) {
            return null;
        }
        try {
            return LocalDateTime.parse(cleaned, EXIF_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isValidCoordinate(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon)
                && lat >= -90.0 && lat <= 90.0
                && lon >= -180.0 && lon <= 180.0;
    }

    private static BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP);
    }

}
