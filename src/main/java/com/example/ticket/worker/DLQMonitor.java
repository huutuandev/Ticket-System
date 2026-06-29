package com.example.ticket.worker;

import com.example.ticket.config.RabbitMQConfig;
import com.example.ticket.event.BookingCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// DLQMonitor.java — file mới
@Slf4j
@Component
public class DLQMonitor {

    @RabbitListener(queues = RabbitMQConfig.BOOKING_DLQ)
    public void handleDeadLetter(BookingCreatedEvent event) {
        log.error("[DLQ] ❌ Message thất bại sau retry — bookingId={}, email={}",
                event.getBookingId(), event.getUserEmail());
        // Production: alert Slack, lưu DB, notify admin...
    }
}
