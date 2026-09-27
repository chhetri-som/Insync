package com.insync.web.controller;


import com.insync.domain.model.User;
import com.insync.service.AlbumService;
import com.insync.web.dto.request.CreateAlbumRequest;
import com.insync.web.dto.request.UpdateAlbumRequest;
import com.insync.web.dto.response.AlbumResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
public class AlbumController {
    private final AlbumService albumService;

    @GetMapping
    public ResponseEntity<List<AlbumResponse>> listAlbums(
            @AuthenticationPrincipal User user ) {
        return ResponseEntity.ok(albumService.listAlbums(user));
    }

    @PostMapping
    public ResponseEntity<AlbumResponse> createAlbum(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateAlbumRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(albumService.createAlbum(user, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlbumResponse> getAlbum(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(albumService.getAlbum(user, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AlbumResponse> updateAlbum(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAlbumRequest request) {
        return ResponseEntity.ok(albumService.updateAlbum(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        albumService.deleteAlbum(user, id);
        return ResponseEntity.noContent().build();
    }
}
