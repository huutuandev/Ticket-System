package com.example.ticket.service.util;

import com.example.ticket.service.EmailTemplateBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Không dùng Mock/@InjectMocks vì class không có dependency
public class EmailTemplateBuilderTest {

    // Kiểm tra build template OTP có chứa đủ mã OTP và thời gian hết hạn
    @Test
    void otpTemplate_success() {
        // Act
        String template = EmailTemplateBuilder.otpTemplate("123456", 5);

        // Assert
        assertTrue(template.contains("123456"));
        assertTrue(template.contains("5 phút"));
        assertTrue(template.contains("Ticket Booking"));
    }

    // Kiểm tra build template booking thành công có chứa đầy đủ thông tin vé
    @Test
    void bookingConfirmedTemplate_success() {
        // Arrange
        LocalDateTime showTime = LocalDateTime.of(2023, 10, 20, 20, 0);

        // Act
        String template = EmailTemplateBuilder.bookingConfirmedTemplate(
                "Nguyen Van A", "Taylor Swift Eras Tour", showTime, "A1, A2", "2,000,000 VND"
        );

        // Assert
        assertTrue(template.contains("Nguyen Van A"));
        assertTrue(template.contains("Taylor Swift Eras Tour"));
        assertTrue(template.contains("A1, A2"));
        assertTrue(template.contains("2,000,000 VND"));
    }

    // Kiểm tra build template vé điện tử có chứa tên khách hàng và link tải PDF
    @Test
    void ticketReadyTemplate_success() {
        // Act
        String template = EmailTemplateBuilder.ticketReadyTemplate("Nguyen Van B", "https://example.com/ticket.pdf");

        // Assert
        assertTrue(template.contains("Nguyen Van B"));
        assertTrue(template.contains("https://example.com/ticket.pdf"));
    }
    
    // Kiểm tra simpleWrapper tạo đúng HTML
    @Test
    void simpleWrapper_success() {
        // Act
        String template = EmailTemplateBuilder.simpleWrapper("Xin chao");
        
        // Assert
        assertTrue(template.contains("Xin chao"));
        assertTrue(template.contains("<p>Xin chao</p>"));
    }
}
