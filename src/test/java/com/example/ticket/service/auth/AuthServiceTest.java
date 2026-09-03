package com.example.ticket.service.auth;

import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RefreshTokenRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.request.VerifyOtpRequest;
import com.example.ticket.dto.response.AuthResponse;
import com.example.ticket.entity.Role;
import com.example.ticket.entity.User;
import com.example.ticket.event.EmailEvent;
import com.example.ticket.repository.RoleRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.jwt.JwtService;
import com.example.ticket.security.user.UserPrincipal;
import com.example.ticket.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        // User có @Builder nên dùng builder
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .status("ACTIVE")
                .password("encodedPassword")
                .build();
    }

    // Kiểm tra đăng ký thành công, tạo user mới và gửi mã OTP qua RabbitMQ
    @Test
    void register_success() {
        // Arrange
        // RegisterRequest không có @Builder nên dùng setter
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setPassword("password");
        req.setFullName("New User");

        // Role không có @Builder nên dùng constructor
        Role userRole = new Role(1L, "USER");

        when(redisTemplate.hasKey("ratelimit:otp:new@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        authService.register(req);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(valueOperations, times(1)).set(eq("otp:new@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        
        ArgumentCaptor<EmailEvent> emailEventCaptor = ArgumentCaptor.forClass(EmailEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), emailEventCaptor.capture());
        assertEquals("new@example.com", emailEventCaptor.getValue().getTo());
    }

    // Kiểm tra đăng ký thất bại do email đã tồn tại
    @Test
    void register_emailExists_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }
    
    // Kiểm tra đăng ký thất bại do dính rate limit tạo OTP
    @Test
    void register_rateLimitExceeded_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        
        when(redisTemplate.hasKey("ratelimit:otp:test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Please wait 1 minute before requesting a new OTP", ex.getMessage());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    // Kiểm tra xác thực OTP thành công, user chuyển sang ACTIVE
    @Test
    void verifyOtp_success() {
        // VerifyOtpRequest không có @Builder nên dùng setter
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@example.com");
        req.setOtp("123456");

        user.setStatus("INACTIVE");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.verifyOtp(req);

        assertEquals("ACTIVE", user.getStatus());
        verify(userRepository, times(1)).save(user);
        verify(redisTemplate, times(1)).delete("otp:test@example.com");
    }
    
    // Kiểm tra xác thực OTP thất bại do OTP sai
    @Test
    void verifyOtp_invalidOtp_throwsException() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@example.com");
        req.setOtp("123456");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@example.com")).thenReturn("654321");

        assertThrows(RuntimeException.class, () -> authService.verifyOtp(req));
        verify(userRepository, never()).save(any());
    }

    // Kiểm tra đăng nhập thành công, trả về access token và refresh token
    @Test
    void login_success() {
        // LoginRequest không có @Builder nên dùng setter
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password");

        Authentication auth = mock(Authentication.class);
        UserPrincipal principal = new UserPrincipal(1L, "test@example.com", "Test User", "encodedPassword", List.of(new SimpleGrantedAuthority("USER")));
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);
        when(jwtService.generateAccessToken(principal)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(principal)).thenReturn("refreshToken");

        AuthResponse res = authService.login(req);

        assertEquals("accessToken", res.getAccessToken());
        assertEquals("refreshToken", res.getRefreshToken());
        assertEquals(1L, res.getUserId());
    }

    // Kiểm tra đăng nhập thất bại do user chưa kích hoạt
    @Test
    void login_inactiveUser_throwsException() {
        user.setStatus("INACTIVE");
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> authService.login(req));
        verify(authenticationManager, never()).authenticate(any());
    }

    // Kiểm tra refresh token thành công, sinh ra token mới
    @Test
    void refreshToken_success() {
        // RefreshTokenRequest không có @Builder nên dùng setter
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("oldRefreshToken");

        when(jwtService.extractUsername("oldRefreshToken")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:token:1")).thenReturn("oldRefreshToken");
        when(jwtService.isTokenValid(eq("oldRefreshToken"), any(UserPrincipal.class))).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("newRefreshToken");

        AuthResponse res = authService.refreshToken(req);

        assertEquals("newAccessToken", res.getAccessToken());
        assertEquals("newRefreshToken", res.getRefreshToken());
    }

    // Kiểm tra refresh token thất bại do token không khớp trên Redis
    @Test
    void refreshToken_invalidToken_throwsException() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("badToken");

        when(jwtService.extractUsername("badToken")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:token:1")).thenReturn("differentToken");

        assertThrows(RuntimeException.class, () -> authService.refreshToken(req));
    }

    // Kiểm tra logout xóa refresh token khỏi Redis
    @Test
    void logout_success() {
        UserPrincipal principal = new UserPrincipal(1L, "test@example.com", "Test User", "pwd", List.of());
        authService.logout(principal);
        verify(redisTemplate, times(1)).delete("refresh:token:1");
    }
}
