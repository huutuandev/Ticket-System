package com.example.ticket.dto.response;


import com.example.ticket.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long bookingId;

    private LocalDateTime bookingDate;

    private BigDecimal totalAmount;

    private String status;

}
