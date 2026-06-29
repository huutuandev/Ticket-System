package com.example.ticket.exception;

public class SeatNotHeldException extends RuntimeException {
  public SeatNotHeldException(String message) {
    super(message);
  }
}
