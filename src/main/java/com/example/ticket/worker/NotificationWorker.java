package com.example.ticket.worker;

import com.example.ticket.event.BookingCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationWorker {

    @RabbitListener(queues = "notification.queue")
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("[NotificationWorker] Push notification cho userId={}, bookingId={}",
                event.getUserId(), event.getBookingId());

        // Giả lập push notification
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[NotificationWorker] ✅ Notification gửi xong cho bookingId={}", event.getBookingId());
    }
}