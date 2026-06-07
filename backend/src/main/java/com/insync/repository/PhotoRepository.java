package com.insync.repository;

import com.insync.domain.enums.PhotoProcessingStatus;
import com.insync.domain.model.Album;
import com.insync.domain.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByAlbumOrderByUploadOrderAsc(Album album);
    List<Photo> findByAlbumAndProcessingStatus(Album album, PhotoProcessingStatus status);
    Optional<Photo> findByIdAndAlbum(UUID photoId, Album album);
    long countByAlbumAndProcessingStatus(Album album, PhotoProcessingStatus status);
    long countByAlbum(Album album);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Photo p SET p.processingStatus = :status WHERE p.album = :album")
    void updateStatusForAlbum(@Param("album") Album album,
                              @Param("status") PhotoProcessingStatus status);

    @Query("SELECT p FROM Photo p LEFT JOIN FETCH p.metadata WHERE p.album = :album AND p.processingStatus = 'DONE' ORDER BY p.uploadOrder ASC")
    List<Photo> findProcessedWithMetadataByAlbum(@Param("album") Album album);

    @Query("SELECT COALESCE(MAX(p.uploadOrder), -1) + 1 FROM Photo p WHERE p.album = :album")
    int getNextUploadOrder(@Param("album") Album album);
}
