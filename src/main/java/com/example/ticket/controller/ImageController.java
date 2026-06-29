package com.example.ticket.controller;

import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.ImageUploadResponse;
import com.example.ticket.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestPart("image") MultipartFile image) {
        String url = imageStorageService.upload(image);
        return ResponseEntity.ok(ApiResponse.success(new ImageUploadResponse(url)));
    }
}
