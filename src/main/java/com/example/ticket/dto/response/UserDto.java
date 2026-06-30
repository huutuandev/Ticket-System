package com.example.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String status;
    private LocalDateTime createdAt;
    private List<String> roles;
}
