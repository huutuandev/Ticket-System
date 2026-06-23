package com.example.ticket.dto.request;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSeatsRequest {

    @NonNull
    private Long rows;

    @NonNull
    private Long seatsPerRow;

}
