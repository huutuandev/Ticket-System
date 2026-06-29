package com.example.ticket.service;

import com.example.ticket.event.BookingCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FakeEmailService {

    public void sendBookingConfirmation(BookingCreatedEvent event) {
        log.info("[FakeEmail] Bắt đầu gửi email tới {} cho bookingId={}",
                event.getUserEmail(), event.getBookingId());

        try {
            // Giả lập gửi mail mất 5 giây
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[FakeEmail] ✅ Gửi email thành công cho bookingId={}", event.getBookingId());
    }
}
