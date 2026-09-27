package com.insync.web.mapper;

import com.insync.domain.model.Album;
import com.insync.web.dto.response.AlbumResponse;

import java.util.List;

public final class AlbumMapper {
    private AlbumMapper() {}

    public static AlbumResponse toResponse(Album album, long photoCount) {
        return new AlbumResponse(
                album.getId(),
                album.getTitle(),
                album.getStyle(),
                album.getStatus(),
                photoCount,
                album.getCreatedAt(),
                album.getUpdatedAt()
        );
    }

    public static List<AlbumResponse> toResponseList(List<Album> albums, java.util.function.Function<Album, Long> photoCountFn) {
        return albums.stream().map(a -> toResponse(a, photoCountFn.apply(a))).toList();
    }
}
