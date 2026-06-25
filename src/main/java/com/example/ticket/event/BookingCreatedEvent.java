package com.example.ticket.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent implements Serializable {

    private Long bookingId;
    private Long userId;
    private String email;
    private BigDecimal totalAmount;
    private LocalDateTime bookingDate;
}
