package com.insync.domain.model;

import com.insync.domain.enums.LayoutType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "album_layouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", nullable = false, length = 20)
    private LayoutType layoutType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // high contrast image -> cover photo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_photo_id")
    private Photo coverPhoto;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<PageLayout> pages = List.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // helpers
    public int pageCount() {
        return pages.size();
    }

    public int totalSlots(){
        return pages.stream().mapToInt(p -> p.slots().size()).sum();
    }

    public List<PageLayout> mergeWith(List<PageLayout> overridePages) {
        if (overridePages == null || overridePages.isEmpty()) {
            return this.pages;
        }

        var overrideMap = overridePages.stream().collect(java.util.stream.Collectors.toMap(PageLayout::pageNumber, p -> p));

        return this.pages.stream().map(basePage -> overrideMap.getOrDefault(basePage.pageNumber(), basePage)).toList();
    }
}
