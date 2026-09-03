package com.example.ticket.worker;

import com.example.ticket.config.RabbitMQConfig;
import com.example.ticket.event.BookingCreatedEvent;
import com.example.ticket.service.EmailService;
import com.example.ticket.service.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailWorker {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_QUEUE)
    public void processBookingEmail(BookingCreatedEvent event) {
        log.info("Consume RabbitMQ message from queue: {}, processing email for {}", RabbitMQConfig.BOOKING_QUEUE, event.getUserEmail());
        try {
            String htmlBody = EmailTemplateBuilder.bookingConfirmedTemplate(
                    event.getUserFullName(),
                    event.getConcertName(),
                    event.getShowTime(),
                    event.getSeats(),
                    event.getTotalAmount().toString()
            );
            emailService.sendHtmlEmail(event.getUserEmail(), "Xác nhận đặt vé thành công", htmlBody);
            log.info("Sent booking email to {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Failed booking email to {}: {}", event.getUserEmail(), e.getMessage(), e);
            throw e;
        }
    }
}
