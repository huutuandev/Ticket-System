package com.example.ticket.controller;

import com.example.ticket.dto.request.PaymentCreateRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.PaymentResponse;
import com.example.ticket.security.user.UserPrincipal;
import com.example.ticket.enums.MockResult;
import com.example.ticket.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentResponse response = paymentService.createPayment(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/mock-pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> mockPay(
            @PathVariable Long id,
            @RequestParam String result
    ) {
        MockResult mockResult;
        if ("success".equalsIgnoreCase(result)) {
            mockResult = MockResult.SUCCESS;
        } else if ("fail".equalsIgnoreCase(result) || "failed".equalsIgnoreCase(result)) {
            mockResult = MockResult.FAILED;
        } else {
            throw new IllegalArgumentException("Invalid result parameter. Use 'success' or 'fail'.");
        }

        PaymentResponse response = paymentService.mockPay(id, mockResult);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long id
    ) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
