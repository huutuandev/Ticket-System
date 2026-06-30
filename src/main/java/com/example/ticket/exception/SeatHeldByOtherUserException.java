package com.example.ticket.exception;

public class SeatHeldByOtherUserException extends RuntimeException {
    public SeatHeldByOtherUserException(String message) {
        super(message);
    }
}
