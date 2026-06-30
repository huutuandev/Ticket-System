package com.example.ticket.controller;

import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RefreshTokenRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.request.VerifyOtpRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.AuthResponse;
import com.example.ticket.security.user.UserPrincipal;
import com.example.ticket.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent to email. Please verify to activate account."));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verify(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
