package com.example.ticket.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * Gửi email dạng HTML (template builder ở EmailTemplateBuilder)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress, "Ticket Booking System");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            log.info("Attempting to send HTML email to {}", to);
            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /** Giữ method cũ để không vỡ code khác đang gọi sendEmail(plain text) */
    public void sendEmail(String to, String subject, String body) {
        sendHtmlEmail(to, subject, EmailTemplateBuilder.simpleWrapper(body));
    }
}