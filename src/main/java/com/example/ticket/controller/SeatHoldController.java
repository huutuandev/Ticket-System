package com.example.ticket.controller;

import com.example.ticket.dto.request.HoldSeatRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.HoldSeatResponse;
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
    public ResponseEntity<ApiResponse<SeatHoldStatusResponse>> getHoldStatus(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(seatService.getHoldStatus(id)));
    }

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<HoldSeatResponse>> holdSeats(
            @Valid @RequestBody HoldSeatRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        HoldSeatResponse response = seatService.holdSeats(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
