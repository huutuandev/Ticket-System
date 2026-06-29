package com.example.ticket.worker;

import com.example.ticket.config.RabbitMQConfig;
import com.example.ticket.event.EmailEvent;
import com.example.ticket.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpEmailWorker {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.OTP_QUEUE)
    public void processOtpEmail(EmailEvent event) {
        log.info("Processing OTP email for {}", event.getTo());
        try {
            emailService.sendHtmlEmail(event.getTo(), event.getSubject(), event.getBody());
            log.info("Sent OTP email to {}", event.getTo());
        } catch (Exception e) {
            log.error("Failed OTP email to {}: {}", event.getTo(), e.getMessage());
            throw e;
        }
    }
}
