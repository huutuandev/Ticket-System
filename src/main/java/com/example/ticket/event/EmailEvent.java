package com.example.ticket.event;

import com.example.ticket.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    private String to;
    private String subject;
    private String body;       // đã là HTML render sẵn, worker không cần biết template
    private EmailType type;    // chỉ để log/trace, KHÔNG dùng để rẽ nhánh template trong worker
}