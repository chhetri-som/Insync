package com.insync.web.mapper;

import com.insync.domain.model.Photo;
import com.insync.web.dto.response.PhotoResponse;

import java.util.List;

public final class PhotoMapper {
    private PhotoMapper() {}

    public static PhotoResponse toResponse(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getAlbum().getId(),
                photo.getOriginalStorageKey(),
                photo.getProcessedStorageKey(),
                photo.getThumbnailStorageKey(),
                photo.getProcessingStatus(),
                photo.getUploadOrder(),
                photo.getCreatedAt()
        );
    }

    public static List<PhotoResponse> toResponseList(List<Photo> photos) {
        return photos.stream().map(PhotoMapper::toResponse).toList();
    }
}
