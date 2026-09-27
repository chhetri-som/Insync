package com.insync.web.controller;

import com.insync.domain.model.User;
import com.insync.service.PhotoService;
import com.insync.web.dto.response.PhotoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/albums/{albumId}/photos")
@RequiredArgsConstructor
public class PhotoController {
    private final PhotoService photoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PhotoResponse>> uploadPhotos(
            @AuthenticationPrincipal User user,
            @PathVariable UUID albumId,
            @RequestParam("files") List<MultipartFile> files) {
        List<PhotoResponse> responses = photoService.uploadPhotos(user, albumId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping
    public ResponseEntity<List<PhotoResponse>> listPhotos(
            @AuthenticationPrincipal User user,
            @PathVariable UUID albumId) {
        return ResponseEntity.ok(photoService.listPhotos(user, albumId));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal User user,
            @PathVariable UUID albumId,
            @PathVariable UUID photoId) {
         photoService.deletePhoto(user, albumId, photoId);
         return ResponseEntity.noContent().build();
    }
}
