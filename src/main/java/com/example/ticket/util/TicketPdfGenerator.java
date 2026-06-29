package com.example.ticket.util;

import com.example.ticket.event.BookingCreatedEvent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class TicketPdfGenerator {

    public byte[] generate(BookingCreatedEvent event, byte[] qrPng) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A5);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Fonts
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Start Text
                contentStream.beginText();
                contentStream.setFont(fontBold, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
                contentStream.showText("E-TICKET XAC NHAN"); // No unicode support, standard text
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(fontRegular, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 90);
                contentStream.setLeading(20f);

                // Note: Standard14Fonts does not support Vietnamese unicode out-of-the-box.
                // We must strip accents to prevent font rendering errors.
                contentStream.showText("Booking ID: " + event.getBookingId());
                contentStream.newLine();
                contentStream.showText("Customer: " + removeAccents(event.getUserFullName()));
                contentStream.newLine();
                contentStream.showText("Event/Movie: " + removeAccents(event.getConcertName()));
                contentStream.newLine();
                contentStream.showText("Showtime: " + removeAccents(DateTimeFormatUtil.forEmail(event.getShowTime())));
                contentStream.newLine();
                contentStream.showText("Seats: " + event.getSeats());
                contentStream.newLine();
                contentStream.showText("Total Amount: " + event.getTotalAmount());
                contentStream.endText();

                // Add QR Code
                PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "qrCode");
                // Position QR code at the bottom
                float qrSize = 150f;
                float qrX = (page.getMediaBox().getWidth() - qrSize) / 2;
                float qrY = 50f;
                contentStream.drawImage(qrImage, qrX, qrY, qrSize, qrSize);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF ticket", e);
        }
    }

    private String removeAccents(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");
        // also replace special vietnamese chars like Đ, đ
        result = result.replace('Đ', 'D').replace('đ', 'd');
        return result;
    }
}
