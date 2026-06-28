package com.example.ticket.exception;

import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleSeatUnavailable(SeatUnavailableException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.SEAT_UNAVAILABLE.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.SEAT_UNAVAILABLE.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.OPTIMISTIC_LOCK.getCode(), ErrorCode.OPTIMISTIC_LOCK.getMessage());
        return ResponseEntity.status(ErrorCode.OPTIMISTIC_LOCK.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(SeatAlreadyHeldException.class)
    public ResponseEntity<ApiResponse<Object>> handleSeatHeld(SeatAlreadyHeldException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.SEAT_ALREADY_HELD.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.SEAT_ALREADY_HELD.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(SeatHoldExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleSeatHoldExpired(SeatHoldExpiredException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.SEAT_HOLD_EXPIRED.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.SEAT_HOLD_EXPIRED.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(SeatNotHeldException.class)
    public ResponseEntity<ApiResponse<Object>> handleSeatNotHeld(SeatNotHeldException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.SEAT_NOT_HELD.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.SEAT_NOT_HELD.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(SeatHeldByOtherUserException.class)
    public ResponseEntity<ApiResponse<Object>> handleSeatHeldByOther(SeatHeldByOtherUserException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.SEAT_HELD_BY_OTHER.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.SEAT_HELD_BY_OTHER.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.PAYMENT_NOT_FOUND.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.PAYMENT_NOT_FOUND.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidPaymentState(InvalidPaymentStateException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.INVALID_PAYMENT_STATE.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_PAYMENT_STATE.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String defaultMessage = "Validation failed";
        if (ex.getBindingResult().getFieldError() != null) {
            defaultMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.INVALID_ARGUMENT.getCode(), defaultMessage);
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        String defaultMessage = "Constraint violation";
        if (ex.getConstraintViolations() != null && !ex.getConstraintViolations().isEmpty()) {
            defaultMessage = ex.getConstraintViolations().iterator().next().getMessage();
        }
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.INVALID_ARGUMENT.getCode(), defaultMessage);
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                ex.getMessage() != null ? ex.getMessage() : "Unexpected runtime error occurred");
        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAll(Exception ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponse);
    }
}
