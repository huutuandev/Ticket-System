package com.example.ticket.exception;

public class ConcertNotFoundException extends RuntimeException {
    public ConcertNotFoundException(Long id) {
        super("Concert not found with id: " + id);
    }
}
