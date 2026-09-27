package com.insync.service;

import com.insync.domain.enums.AlbumStyle;
import com.insync.domain.model.Album;
import com.insync.domain.model.User;
import com.insync.exception.ResourceNotFoundException;
import com.insync.repository.AlbumRepository;
import com.insync.repository.PhotoRepository;
import com.insync.web.dto.request.CreateAlbumRequest;
import com.insync.web.dto.request.UpdateAlbumRequest;
import com.insync.web.dto.response.AlbumResponse;
import com.insync.web.mapper.AlbumMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {
    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public AlbumResponse createAlbum(User owner, CreateAlbumRequest request) {
        Album album = Album.builder()
                .owner(owner)
                .title(request.title())
                .style(request.style() != null ? request.style() : AlbumStyle.CHRONOLOGICAL)
                .build();

        Album saved = albumRepository.save(album);
        log.debug("Created album {} for user {}", saved.getId(), owner.getId());
        return AlbumMapper.toResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> listAlbums(User owner) {
        List<Album> albums = albumRepository.findByOwnerOrderByCreatedAtDesc(owner);
        return AlbumMapper.toResponseList(albums, a -> photoRepository.countByAlbum(a));
    }

    @Transactional(readOnly = true)
    public AlbumResponse getAlbum(User owner, UUID albumId) {
        Album album = findOwned(owner, albumId);
        long count = photoRepository.countByAlbum(album);
        return AlbumMapper.toResponse(album, count);
    }

    @Transactional
    public AlbumResponse updateAlbum(User owner, UUID albumId, UpdateAlbumRequest request) {
        Album album = findOwned(owner, albumId);
        if (request.title() != null) {
            album.setTitle(request.title());
        }
        if (request.style() != null) {
            album.setStyle(request.style());
        }
        Album saved = albumRepository.save(album);
        long count = photoRepository.countByAlbum(saved);
        return AlbumMapper.toResponse(saved, count);
    }

    @Transactional
    public void deleteAlbum(User owner, UUID albumId) {
        Album album = findOwned(owner, albumId);
        albumRepository.delete(album);
        log.debug("Deleted album {}", albumId);
        // Photos and layouts are removed by ON DELETE CASCADE
    }

    // helper
    @Transactional(readOnly = true)
    public Album findOwned(User owner, UUID albumId) {
        return albumRepository.findByIdAndOwner(albumId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Album", albumId));
    }
}
