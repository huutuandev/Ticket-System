package com.example.ticket.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateConcertRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String location;

    @NotNull
    private LocalDateTime eventTime;


    private String description;

    @NotBlank
    private String imageUrl;

}
