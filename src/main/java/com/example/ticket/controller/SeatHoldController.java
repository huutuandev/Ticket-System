package com.example.ticket.controller;

import com.example.ticket.dto.request.HoldSeatRequest;
import com.example.ticket.dto.response.SeatHoldStatusResponse;
import com.example.ticket.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.ticket.security.user.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatService seatService;

    @GetMapping("/{id}/hold-status")
    public ResponseEntity<SeatHoldStatusResponse> getHoldStatus(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(seatService.getHoldStatus(id));
    }

    @PostMapping("/hold")
    public ResponseEntity<Void> holdSeats(
            @Valid @RequestBody HoldSeatRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        seatService.holdSeats(request, principal.getId());
        return ResponseEntity.ok().build();
    }
}
