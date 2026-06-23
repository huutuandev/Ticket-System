package com.example.ticket.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Long id;

    private String rowName;

    private Long seatNumber;

    private String type;

    private String status;

    private BigDecimal price;
}
