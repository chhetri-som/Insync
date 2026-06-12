package com.insync.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "photo_metadata")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // maps to exact photo
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id", nullable = false, unique = true)
    private Photo photo;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    // GPS latitude
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    // GPS longitude from EXIF. Null if not present or stripped
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // K-Means dominant colors
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dominant_colors", columnDefinition = "jsonb")
    private List<DominantColor> dominantColors;

    // Average pixel brightness normalized 0.0-1.0
    @Column(name = "brightness_score")
    private Double brightnessScore;

    // RMS contrast score 0.0-1.0
    @Column(name = "contrast_score")
    private Double contrastScore;

    // Edge density score 0.0-1.0
    @Column(name = "complexity_score")
    private Double complexityScore;

    // true if mediapipe detected one face
    @Column(name = "has_faces", nullable = false)
    @Builder.Default
    private Boolean hasFaces = false;

    // no. of faces
    @Column(name = "face_count", nullable = false)
    @Builder.Default
    private Integer faceCount = 0;

}
