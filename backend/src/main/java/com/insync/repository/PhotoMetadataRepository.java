package com.insync.repository;

import com.insync.domain.model.Photo;
import com.insync.domain.model.PhotoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoMetadataRepository extends JpaRepository<PhotoMetadata, UUID> {

    Optional<PhotoMetadata> findByPhoto(Photo photo);
    boolean existsByPhoto(Photo photo);
}
