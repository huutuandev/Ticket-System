package com.example.ticket.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(500, "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR),
    RESOURCE_NOT_FOUND(404, "Resource not found", HttpStatus.NOT_FOUND),
    SEAT_UNAVAILABLE(409, "Seat is unavailable", HttpStatus.CONFLICT),
    SEAT_ALREADY_HELD(409, "Seat already held", HttpStatus.CONFLICT),
    SEAT_HOLD_EXPIRED(410, "Hold expired or invalid", HttpStatus.GONE),
    SEAT_NOT_HELD(400, "Seat is not held", HttpStatus.BAD_REQUEST),
    SEAT_HELD_BY_OTHER(409, "Seat held by another user", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK(409, "Seat was booked by another user. Please try again", HttpStatus.CONFLICT),
    INVALID_ARGUMENT(400, "Invalid input validation", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND(404, "Payment not found", HttpStatus.NOT_FOUND),
    INVALID_PAYMENT_STATE(400, "Invalid payment state", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;
}
