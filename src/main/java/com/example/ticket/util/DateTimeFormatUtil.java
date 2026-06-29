package com.example.ticket.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeFormatUtil {

    // "20:00, Thứ Hai 29/06/2026"
    private static final DateTimeFormatter VN_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm, EEEE dd/MM/yyyy", new Locale("vi", "VN"));

    // "20:00 29/06/2026" — dùng cho PDF (Standard14Fonts không hỗ trợ Unicode "Thứ Hai")
    private static final DateTimeFormatter PDF_SAFE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public static String forEmail(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return capitalize(dateTime.format(VN_DISPLAY_FORMAT));
    }

    public static String forPdf(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(PDF_SAFE_FORMAT);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
