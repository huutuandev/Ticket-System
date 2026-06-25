package com.example.ticket.worker;

import com.example.ticket.event.BookingCreatedEvent;
import com.example.ticket.service.FakeEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailWorker {

    private final FakeEmailService fakeEmailService;

    @RabbitListener(queues = "booking.queue")
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("[EmailWorker] Received event for bookingId={}", event.getBookingId());


//        System.out.println("Send email booking: " + event.getBookingId());
//
//
//        fakeEmailService.sendBookingConfirmation(event);
        throw new RuntimeException("Giả lập lỗi gửi email!");
    }

    // Trong EmailWorker hoặc tạo file riêng DLQMonitor.java
    @RabbitListener(queues = "booking.dlq")
    public void handleDeadLetter(BookingCreatedEvent event) {
        log.error("[DLQ] ❌ Message thất bại sau 3 lần retry — bookingId={}, email={}",
                event.getBookingId(), event.getEmail());
        // Production: alert Slack, lưu DB, gửi email admin...
    }
}
