package com.example.ticket.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldSeatResponse {
    private List<HeldSeatDto> heldSeats;
    private BigDecimal totalAmount;
    private LocalDateTime holdExpiresAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeldSeatDto {
        private Long seatId;
        private String seatCode;
        private BigDecimal price;
    }
}
