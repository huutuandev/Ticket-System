package com.example.ticket.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        String publicId = UUID.randomUUID().toString();
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "folder", "ticket-system"
        ));
        return uploadResult.get("secure_url").toString();
    }
}
