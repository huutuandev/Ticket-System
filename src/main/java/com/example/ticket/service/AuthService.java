package com.example.ticket.service;

import com.example.ticket.config.RabbitMQConfig;
import com.example.ticket.enums.EmailType;
import com.example.ticket.event.EmailEvent;
import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RefreshTokenRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.request.VerifyOtpRequest;
import com.example.ticket.dto.response.AuthResponse;
import com.example.ticket.entity.Role;
import com.example.ticket.entity.User;
import com.example.ticket.repository.RoleRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.jwt.JwtService;
import com.example.ticket.security.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String OTP_PREFIX = "otp:";

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Starting registration for email: {}", request.getEmail());
        
        // Rate limiting for OTP
        String rateLimitKey = "ratelimit:otp:" + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            log.warn("Rate limit exceeded for OTP request for email: {}", request.getEmail());
            throw new RuntimeException("Please wait 1 minute before requesting a new OTP");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already taken: {}", request.getEmail());
            throw new RuntimeException("Email is already taken");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("INACTIVE");

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
        log.info("User saved with INACTIVE status: {}", request.getEmail());

        // Generate and store OTP
        String otp = generateOtp();
        redisTemplate.opsForValue().set(OTP_PREFIX + request.getEmail(), otp, 5, TimeUnit.MINUTES);
        
        // Set rate limit (1 minute)
        redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);

        // Send OTP via RabbitMQ
        EmailEvent otpEvent = new EmailEvent(
                request.getEmail(),
                "Your OTP Code",
                EmailTemplateBuilder.otpTemplate(otp, 5),   // render HTML ngay tại đây
                EmailType.OTP
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.OTP_EXCHANGE, RabbitMQConfig.ROUTING_OTP_SENT, otpEvent);
        log.info("OTP sent to RabbitMQ for email: {}", request.getEmail());
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        String key = OTP_PREFIX + request.getEmail();
        String savedOtp = redisTemplate.opsForValue().get(key);

        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("ACTIVE");
        userRepository.save(user);

        redisTemplate.delete(key);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if ("INACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not activated !");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String jwtToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        log.info("User {} logged in successfully", request.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .fullName(principal.getFullName())
                .userId(principal.getId())
                .role(principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String redisKey = "refresh:token:" + user.getId();
        String savedToken = redisTemplate.opsForValue().get(redisKey);

        if (savedToken == null || !savedToken.equals(token)) {
            throw new RuntimeException("Invalid refresh token");
        }

        UserPrincipal principal = UserPrincipal.create(user);

        if (!jwtService.isTokenValid(token, principal)) {
            throw new RuntimeException("Refresh token is expired or invalid");
        }

        String accessToken = jwtService.generateAccessToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(UserPrincipal principal) {
        String redisKey = "refresh:token:" + principal.getId();
        redisTemplate.delete(redisKey);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
