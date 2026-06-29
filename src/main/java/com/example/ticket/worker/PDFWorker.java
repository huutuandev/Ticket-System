package com.example.ticket.worker;

import com.example.ticket.event.BookingCreatedEvent;
import com.example.ticket.service.CloudinaryService;
import com.example.ticket.service.EmailService;
import com.example.ticket.service.EmailTemplateBuilder;
import com.example.ticket.util.QrCodeGenerator;
import com.example.ticket.util.TicketPdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PDFWorker {

    private final TicketPdfGenerator ticketPdfGenerator;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    @RabbitListener(queues = "pdf.queue")
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("[PDFWorker] Tạo PDF cho bookingId={}", event.getBookingId());

        try {
            // 1. Generate QR code
            String qrContent = "BOOKING:" + event.getBookingId();
            byte[] qrPng = QrCodeGenerator.generate(qrContent, 300);

            // 2. Generate PDF
            byte[] pdfBytes = ticketPdfGenerator.generate(event, qrPng);

            // 3. Upload to Cloudinary
            String publicId = "booking_" + event.getBookingId();
            String ticketUrl = cloudinaryService.uploadPdf(pdfBytes, publicId);
            
            log.info("[PDFWorker] ✅ PDF tạo và upload xong cho bookingId={}, ticketUrl={}", event.getBookingId(), ticketUrl);

            // 4. Send email
            String html = EmailTemplateBuilder.ticketReadyTemplate(event.getUserFullName(), ticketUrl);
            emailService.sendHtmlEmail(event.getUserEmail(), "Vé điện tử của bạn", html);

        } catch (Exception e) {
            log.error("[PDFWorker] Lỗi khi tạo PDF hoặc gửi mail cho bookingId={}, error={}", event.getBookingId(), e.getMessage());
            throw new RuntimeException("Lỗi xử lý PDFWorker cho bookingId: " + event.getBookingId(), e);
        }
    }
}
