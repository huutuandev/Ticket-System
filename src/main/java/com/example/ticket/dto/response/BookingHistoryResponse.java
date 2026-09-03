package com.example.ticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryResponse {
    private Long bookingId;
    private Long concertId;
    private String concertName;
    private LocalDateTime showTime;
    private String posterUrl;
    private List<SeatInfo> seats;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
        private String seatCode;
        private BigDecimal price;
    }
}
