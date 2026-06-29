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
public class PaymentResponse {
    private Long id;
    private String status;
    private String transactionId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private List<Long> seatIds;
    private Long userId;
    private String holdId;
}
