package com.example.ticket.dto.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatHoldStatusResponse {
    private Long seatId;
    private boolean held;
    private Long remainingSeconds;
}
