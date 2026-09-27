package com.insync.service;


import com.insync.domain.enums.AlbumStatus;
import com.insync.domain.model.Album;
import com.insync.domain.model.Photo;
import com.insync.domain.model.User;
import com.insync.exception.ProcessingException;
import com.insync.exception.ResourceNotFoundException;
import com.insync.repository.PhotoRepository;
import com.insync.service.storage.StorageService;
import com.insync.web.dto.response.PhotoResponse;
import com.insync.web.mapper.PhotoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {
    private static final int MAX_PHOTOS_PER_UPLOAD = 50;

    private final PhotoRepository photoRepository;
    private final AlbumService albumService;
    private final StorageService storageService;

    @Transactional
    public List<PhotoResponse> uploadPhotos(User owner, UUID albumId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required.");
        }
        if (files.size() > MAX_PHOTOS_PER_UPLOAD) {
            throw new IllegalArgumentException(
                    "Cannot upload more than " + MAX_PHOTOS_PER_UPLOAD + " photos at once.");
        }
        Album album = albumService.findOwned(owner, albumId);

        int nextOrder = photoRepository.getNextUploadOrder(album);
        List<Photo> saved = new ArrayList<>(files.size());

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file mustn't be empty.");
            }

            String storageKey;
            try {
                storageKey = storageService.storeOriginal(file);
            } catch (IOException e) {
                throw new ProcessingException("Failed to store file: " + file.getOriginalFilename(), e);
            }

            Photo photo = Photo.builder()
                    .album(album)
                    .originalStorageKey(storageKey)
                    .uploadOrder(nextOrder++)
                    .build();

            saved.add(photoRepository.save(photo));
        }

        album.setStatus(AlbumStatus.PROCESSING);

        log.debug("Uploaded {} photo(s) to album {}", saved.size(), albumId);

        // TODO: PhotoProcessingService
        return PhotoMapper.toResponseList(saved);
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> listPhotos(User owner, UUID albumId) {
        Album album = albumService.findOwned(owner, albumId);
        List<Photo> photos = photoRepository.findByAlbumOrderByUploadOrderAsc(album);
        return PhotoMapper.toResponseList(photos);
    }

    @Transactional
    public void deletePhoto(User owner, UUID albumId, UUID photoId) {
        Album album = albumService.findOwned(owner, albumId);
        Photo photo = photoRepository.findByIdAndAlbum(photoId, album)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        try {
            storageService.deleteAll(
                    photo.getOriginalStorageKey(),
                    photo.getProcessedStorageKey(),
                    photo.getThumbnailStorageKey()
            );
        } catch (IOException e) {
            log.warn("Could not delete storage files for photo {}: {}", photoId, e.getMessage());
        }

        photoRepository.delete(photo);
        log.debug("Deleted photo {} from album {}", photoId, albumId);
    }
}
