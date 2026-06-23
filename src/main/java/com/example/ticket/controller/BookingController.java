package com.example.ticket.controller;

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

    @PostMapping
    public ResponseEntity<BookingResponse> bookingSeats(@RequestBody @Valid CreateBookingRequest request){
        return ResponseEntity.ok(bookingService.createBooking(request));
    }
}
