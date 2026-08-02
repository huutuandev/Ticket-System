package com.example.ticket.service.util;

import com.example.ticket.event.BookingCreatedEvent;
import com.example.ticket.service.FakeEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FakeEmailServiceTest {

    @InjectMocks
    private FakeEmailService fakeEmailService;

    // Kiểm tra việc giả lập gửi email chạy bình thường nhưng có Thread.sleep
    // Dùng thread để interrupt tránh làm chậm test suite
    @Test
    void sendBookingConfirmation_successWithInterruption() throws InterruptedException {
        // Arrange
        // BookingCreatedEvent không có @Builder, sử dụng constructor đầy đủ
        BookingCreatedEvent event = new BookingCreatedEvent(
                100L, 1L, "test@example.com", "Test User", "Test Concert",
                LocalDateTime.now(), "A1", BigDecimal.valueOf(100000), LocalDateTime.now()
        );
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        // Act
        Thread thread = new Thread(() -> {
            fakeEmailService.sendBookingConfirmation(event);
            isCompleted.set(true);
        });
        
        thread.start();
        Thread.sleep(100); // Đợi thread bắt đầu
        thread.interrupt(); // Interrupt để bỏ qua Thread.sleep(5000)
        thread.join(1000); // Chờ thread kết thúc

        // Assert
        assertTrue(isCompleted.get(), "Method nên hoàn thành sau khi bị interrupt mà không ném exception ra ngoài");
    }
}
