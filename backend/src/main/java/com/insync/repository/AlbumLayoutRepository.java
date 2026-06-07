package com.insync.repository;

import com.insync.domain.enums.LayoutType;
import com.insync.domain.model.Album;
import com.insync.domain.model.AlbumLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumLayoutRepository extends JpaRepository<AlbumLayout, UUID> {

    Optional<AlbumLayout> findByAlbumAndLayoutTypeAndIsActiveTrue( Album album, LayoutType layoutType );
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AlbumLayout al SET al.isActive = false WHERE al.album = :album AND al.layoutType = :type")
    void deactivateExistingLayouts(@Param("album") Album album, @Param("type") LayoutType type);
}
