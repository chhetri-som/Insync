package com.insync.domain.model;

import com.insync.domain.enums.PhotoProcessingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "photos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    // relative key to find raw uploads via StorageService
    @Column(name = "original_storage_key", nullable = false, length = 500)
    private String originalStorageKey;

    // set after photos are edited.
    @Column(name = "processed_storage_key", length = 500)
    private String processedStorageKey;

    // set after thumbnail generation.
    @Column(name = "thumbnail_storage_key", length = 500)
    private String thumbnailStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    @Builder.Default
    private PhotoProcessingStatus processingStatus = PhotoProcessingStatus.PENDING;

    // used as tie-breaker sort when EXIF timestamps is missing
    @Column(name = "upload_order", nullable = false)
    @Builder.Default
    private Integer uploadOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // set No to cascade delete to avoid double-delete race condition (constraint in DB)
    @OneToOne(mappedBy = "photo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PhotoMetadata metadata;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // set true if all three processing steps are complete
    public boolean isProcessed() {
        return PhotoProcessingStatus.DONE.equals(this.processingStatus);
    }

    // processed version if ready else original key
    public String getBestAvailableKey() {
        return processedStorageKey != null ? processedStorageKey : originalStorageKey;
    }
 }
