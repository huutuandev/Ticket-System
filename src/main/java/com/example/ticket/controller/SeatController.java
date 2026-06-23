package com.example.ticket.controller;


import com.example.ticket.dto.request.GenerateSeatsRequest;
import com.example.ticket.dto.response.SeatResponse;
import com.example.ticket.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/concerts/{concertId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateSeats(@PathVariable Long concertId, @RequestBody @Valid GenerateSeatsRequest  request) {
        seatService.generateSeats(request, concertId);
        return ResponseEntity.ok("Generated");
    }

    @GetMapping
    public List<SeatResponse> getSeats(@PathVariable Long concertId) {
        return seatService.getSeatsByConcert(concertId);
    }
}
