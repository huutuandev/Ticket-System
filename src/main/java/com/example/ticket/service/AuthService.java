package com.example.ticket.service;

import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RefreshTokenRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.response.AuthResponse;
import com.example.ticket.entity.Role;
import com.example.ticket.entity.User;
import com.example.ticket.repository.RoleRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.jwt.JwtService;
import com.example.ticket.security.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        // Convert sang UserPrincipal trước khi generate token
        UserPrincipal principal = UserPrincipal.create(user);

        String jwtToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

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

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Lấy principal từ Authentication object được trả về
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String jwtToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .fullName(principal.getFullName()) // thêm field này vào UserPrincipal nếu chưa có
                .userId(principal.getId())
                .role(principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        String username = jwtService.extractUsername(token);

        User user = userRepository.findByUsername(username)
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
}
