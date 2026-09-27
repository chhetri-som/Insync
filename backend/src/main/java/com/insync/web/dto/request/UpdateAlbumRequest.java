package com.insync.web.dto.request;

import com.insync.domain.enums.AlbumStyle;
import jakarta.validation.constraints.Size;

public record UpdateAlbumRequest(

        @Size(min=1, max=255, message = "Title must be between 1 and 255 characters.")
        String title,
        AlbumStyle style
) {
}
