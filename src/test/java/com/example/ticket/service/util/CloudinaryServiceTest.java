package com.example.ticket.service.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.ticket.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    // Kiểm tra upload PDF thành công, trả về secure_url
    @Test
    void uploadPdf_success() throws IOException {
        // Arrange
        byte[] pdfBytes = "dummy pdf content".getBytes();
        String publicId = "ticket-123";
        Map<String, Object> uploadResult = Map.of("secure_url", "https://cloudinary.com/tickets/ticket-123.pdf");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(pdfBytes), anyMap())).thenReturn(uploadResult);

        // Act
        String resultUrl = cloudinaryService.uploadPdf(pdfBytes, publicId);

        // Assert
        assertEquals("https://cloudinary.com/tickets/ticket-123.pdf", resultUrl);
        verify(uploader, times(1)).upload(eq(pdfBytes), anyMap());
    }

    // Kiểm tra upload PDF thất bại, ném ra RuntimeException
    @Test
    void uploadPdf_throwsIOException() throws IOException {
        // Arrange
        byte[] pdfBytes = "dummy pdf content".getBytes();
        String publicId = "ticket-123";

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("Upload failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> cloudinaryService.uploadPdf(pdfBytes, publicId));
        assertEquals("Failed to upload PDF to Cloudinary", exception.getMessage());
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IOException);
    }
}
