package com.example.ticket.service.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.ticket.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private ImageService imageService;

    // Kiểm tra upload image thành công, trả về secure_url
    @Test
    void uploadImage_success() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image data".getBytes());
        Map<String, Object> uploadResult = Map.of("secure_url", "https://cloudinary.com/images/test.jpg");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        // Act
        String resultUrl = imageService.uploadImage(file);

        // Assert
        assertEquals("https://cloudinary.com/images/test.jpg", resultUrl);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    // Kiểm tra upload image thất bại do file lỗi, ném ra IOException
    @Test
    void uploadImage_throwsIOException() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("Read failed"));

        // Act & Assert
        assertThrows(IOException.class, () -> imageService.uploadImage(file));
        verify(cloudinary, never()).uploader();
    }
}
