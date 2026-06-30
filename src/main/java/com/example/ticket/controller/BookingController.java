package com.example.ticket.controller;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.security.user.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // 🔹 Optimistic Locking
    @PostMapping("/optimistic")
    public ResponseEntity<ApiResponse<BookingResponse>> bookingSeatsOptimistic(
            @RequestBody @Valid CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) throws InterruptedException {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.createBookingOptimistic(request, principal.getId()))
        );
    }

    // 🔹 Pessimistic Locking
    @PostMapping("/pessimistic")
    public ResponseEntity<ApiResponse<BookingResponse>> bookingSeatsPessimistic(
            @RequestBody @Valid CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) throws InterruptedException {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.createBookingPessimistic(request, principal.getId()))
        );
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @RequestBody @Valid ConfirmBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.confirmBooking(request, principal.getId()))
        );
    }
}
