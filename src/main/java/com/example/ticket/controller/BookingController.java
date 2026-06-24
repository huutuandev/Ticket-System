package com.example.ticket.controller;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // 🔹 Optimistic Locking
    @PostMapping("/optimistic")
    public ResponseEntity<BookingResponse> bookingSeatsOptimistic(
            @RequestBody @Valid CreateBookingRequest request
    ) throws InterruptedException {

        return ResponseEntity.ok(
                bookingService.createBookingOptimistic(request)
        );
    }

    // 🔹 Pessimistic Locking
    @PostMapping("/pessimistic")
    public ResponseEntity<BookingResponse> bookingSeatsPessimistic(
            @RequestBody @Valid CreateBookingRequest request
    ) throws InterruptedException {

        return ResponseEntity.ok(
                bookingService.createBookingPessimistic(request)
        );
    }

    @PostMapping("/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @RequestBody @Valid ConfirmBookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.confirmBooking(request));
    }
}

