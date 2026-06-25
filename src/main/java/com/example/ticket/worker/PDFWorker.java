package com.example.ticket.worker;

import com.example.ticket.event.BookingCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PDFWorker {

    @RabbitListener(queues = "pdf.queue")
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("[PDFWorker] Tạo PDF cho bookingId={}", event.getBookingId());

        // Giả lập tạo PDF
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[PDFWorker] ✅ PDF tạo xong cho bookingId={}", event.getBookingId());
    }
}
