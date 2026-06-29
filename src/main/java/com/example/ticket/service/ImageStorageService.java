package com.example.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final ImageService imageService;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB


    public String upload(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Image is required");
        }
        
        if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
            throw new RuntimeException("Invalid image type. Only JPEG, PNG and WEBP are allowed.");
        }
        
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Image size exceeds the maximum limit of 5MB");
        }

        try {
            return imageService.uploadImage(image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }
}
