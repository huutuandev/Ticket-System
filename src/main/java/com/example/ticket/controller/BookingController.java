package com.example.ticket.controller;

import com.example.ticket.dto.request.ConfirmBookingRequest;
import com.example.ticket.dto.request.CreateBookingRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.BookingResponse;
import com.example.ticket.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.ticket.dto.response.BookingHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<BookingHistoryResponse>>> getBookingHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "bookingDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BookingHistoryResponse> history = bookingService.getBookingHistory(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
