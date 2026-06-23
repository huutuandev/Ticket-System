package com.example.ticket.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NonNull
    private Long userId;

    @NotEmpty
    private List<Long> seatIds;
}
