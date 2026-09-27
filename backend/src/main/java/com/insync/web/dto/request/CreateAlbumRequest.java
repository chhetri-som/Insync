package com.insync.web.dto.request;

import com.insync.domain.enums.AlbumStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAlbumRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title must not exceed 255 characters.")
        String title,

        AlbumStyle style // default -> CHRONOLOGICAL
) {
}
