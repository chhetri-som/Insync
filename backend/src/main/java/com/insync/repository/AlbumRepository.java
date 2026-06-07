package com.insync.repository;

import com.insync.domain.enums.AlbumStatus;
import com.insync.domain.model.Album;
import com.insync.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    List<Album> findByOwnerOrderByCreatedAtDesc(User owner);
    Optional<Album> findByIdAndOwner(UUID id, User owner);
    boolean existsByIdAndOwner(UUID id, User owner);

    @Query("SELECT a FROM Album a WHERE a.status = :status")
    List<Album> findByStatus(@Param("status") AlbumStatus status);

    long countByOwner(User owner);
}
