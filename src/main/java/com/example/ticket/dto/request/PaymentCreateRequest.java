package com.example.ticket.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {
    @NotEmpty(message = "Seat IDs list cannot be empty")
    private List<Long> seatIds;

    @NotNull
    private BigDecimal totalAmount;
}
