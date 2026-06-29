package com.example.ticket.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConcertRequest {

    private String name;

    private String location;

    private LocalDateTime eventTime;

    private String description;

    private String imageUrl;

}
