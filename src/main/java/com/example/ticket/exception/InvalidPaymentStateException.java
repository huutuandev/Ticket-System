package com.example.ticket.exception;

public class InvalidPaymentStateException extends RuntimeException {
    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
