package com.example.ticket.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSeatsRequest {

    @NotNull
    private Long rows;

    @NotNull
    private Long seatsPerRow;

}
