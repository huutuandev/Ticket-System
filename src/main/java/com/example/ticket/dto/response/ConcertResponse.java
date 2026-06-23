package com.example.ticket.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResponse {

    private Long id;

    private String name;

    private String location;

    private LocalDateTime eventTime;

    private String description;

    private LocalDateTime creatAt;
}
