package com.example.ticket.service.util;

import com.example.ticket.service.ImageService;
import com.example.ticket.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImageStorageServiceTest {

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageStorageService imageStorageService;

    // Kiểm tra upload hình ảnh hợp lệ thành công
    @Test
    void upload_success() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy data".getBytes());
        when(imageService.uploadImage(file)).thenReturn("https://example.com/test.jpg");

        // Act
        String result = imageStorageService.upload(file);

        // Assert
        assertEquals("https://example.com/test.jpg", result);
        verify(imageService, times(1)).uploadImage(file);
    }

    // Kiểm tra upload thất bại khi file null
    @Test
    void upload_fileNull_throwsException() {
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.upload(null));
        assertEquals("Image is required", ex.getMessage());
    }

    // Kiểm tra upload thất bại khi file trống
    @Test
    void upload_fileEmpty_throwsException() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.upload(file));
        assertEquals("Image is required", ex.getMessage());
    }

    // Kiểm tra upload thất bại do sai định dạng (ví dụ text/plain)
    @Test
    void upload_invalidContentType_throwsException() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy data".getBytes());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.upload(file));
        assertEquals("Invalid image type. Only JPEG, PNG and WEBP are allowed.", ex.getMessage());
    }

    // Kiểm tra upload thất bại do kích thước quá lớn (>5MB)
    @Test
    void upload_sizeExceeds_throwsException() {
        // Arrange
        byte[] largeData = new byte[6 * 1024 * 1024]; // 6MB
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", largeData);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.upload(file));
        assertEquals("Image size exceeds the maximum limit of 5MB", ex.getMessage());
    }

    // Kiểm tra upload ném ra IOException từ ImageService
    @Test
    void upload_imageServiceThrowsIOException() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy data".getBytes());
        when(imageService.uploadImage(file)).thenThrow(new IOException("Network error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.upload(file));
        assertEquals("Failed to upload image", ex.getMessage());
    }
}
