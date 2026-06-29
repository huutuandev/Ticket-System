package com.example.ticket.service;

/**
 * Builder các template HTML cho email — không phụ thuộc Thymeleaf,
 * dùng text block Java thuần để dễ copy/sửa.
 */
public class EmailTemplateBuilder {

    private static final String WRAPPER_OPEN = """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background-color:#f4f4f7;font-family:Segoe UI,Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f7;padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background:#1a73e8;padding:20px 32px;">
                          <span style="color:#fff;font-size:18px;font-weight:600;">🎬 Ticket Booking</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;color:#333;font-size:15px;line-height:1.6;">
            """;

    private static final String WRAPPER_CLOSE = """
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:16px 32px;background:#fafafa;color:#999;font-size:12px;text-align:center;">
                          Email này được gửi tự động, vui lòng không phản hồi.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    public static String simpleWrapper(String message) {
        return WRAPPER_OPEN + "<p>" + message + "</p>" + WRAPPER_CLOSE;
    }

    /** Template OTP — code hiển thị to, rõ, có thời hạn */
    public static String otpTemplate(String otp, int expireMinutes) {
        String content = """
                <p>Xin chào,</p>
                <p>Mã xác thực (OTP) của bạn là:</p>
                <div style="text-align:center;margin:24px 0;">
                  <span style="display:inline-block;background:#f0f4ff;color:#1a73e8;
                               font-size:28px;font-weight:700;letter-spacing:6px;
                               padding:14px 28px;border-radius:6px;">%s</span>
                </div>
                <p>Mã có hiệu lực trong <b>%d phút</b>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                <p>Nếu bạn không yêu cầu mã này, hãy bỏ qua email.</p>
                """.formatted(otp, expireMinutes);
        return WRAPPER_OPEN + content + WRAPPER_CLOSE;
    }

    /** Template xác nhận booking thành công */
    public static String bookingConfirmedTemplate(String customerName, String movieName,
                                                  String showTime, String seats, String totalPrice) {
        String content = """
                <p>Xin chào <b>%s</b>,</p>
                <p>Đặt vé của bạn đã được xác nhận thành công 🎉</p>
                <table width="100%%" cellpadding="6" style="margin:20px 0;font-size:14px;">
                  <tr><td style="color:#888;">Phim</td><td><b>%s</b></td></tr>
                  <tr><td style="color:#888;">Giờ chiếu</td><td>%s</td></tr>
                  <tr><td style="color:#888;">Ghế</td><td>%s</td></tr>
                  <tr><td style="color:#888;">Tổng tiền</td><td><b style="color:#1a73e8;">%s</b></td></tr>
                </table>
                <p>Vé điện tử (PDF) sẽ được gửi trong email kế tiếp.</p>
                """.formatted(customerName, movieName, showTime, seats, totalPrice);
        return WRAPPER_OPEN + content + WRAPPER_CLOSE;
    }
}
