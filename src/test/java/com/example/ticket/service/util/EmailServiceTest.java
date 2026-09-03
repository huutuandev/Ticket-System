package com.example.ticket.service.util;

import com.example.ticket.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@ticket.com");
    }

    // Kiểm tra gửi email HTML thành công
    @Test
    void sendHtmlEmail_success() {
        // Arrange
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendHtmlEmail("user@example.com", "Test Subject", "<h1>Hello</h1>");

        // Assert
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    // Kiểm tra gửi email lỗi MessagingException ném ra RuntimeException
    @Test
    void sendHtmlEmail_throwsException() {
        // Arrange
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server down")).when(javaMailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> emailService.sendHtmlEmail("user@example.com", "Test Subject", "<h1>Hello</h1>"));
        assertEquals("Failed to send email", exception.getMessage());
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    // Kiểm tra gửi text email thông thường thành công (sử dụng wrapper)
    @Test
    void sendEmail_success() {
        // Arrange
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("user@example.com", "Test Subject", "Plain text hello");

        // Assert
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(mimeMessage);
    }
}
