package demo.project.service.impl;

import demo.project.dto.request.ChangePasswordRequest;
import demo.project.dto.request.LoginRequest;
import demo.project.dto.request.RefreshTokenRequest;
import demo.project.dto.request.RegisterRequest;
import demo.project.dto.response.AuthResponse;
import demo.project.dto.response.UserResponse;
import demo.project.entity.RefreshToken;
import demo.project.entity.TokenBlacklist;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.RefreshTokenRepository;
import demo.project.repository.TokenBlacklistRepository;
import demo.project.repository.UserRepository;
import demo.project.security.jwt.JwtProperties;
import demo.project.security.jwt.JwtService;
import demo.project.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
            .filter(User::isEnabled)
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
            .token(refreshToken)
            .user(user)
            .expiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(jwtProperties.getRefreshTokenDays() * 86400), ZoneOffset.UTC))
            .revoked(false)
            .build());

        return buildAuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        String accessToken = jwtService.generateAccessToken(refreshToken.getUser().getUsername(), refreshToken.getUser().getRole());
        return buildAuthResponse(accessToken, request.getRefreshToken());
    }

    @Override
    @Transactional
    public void logout(String bearerToken) {
        String token = extractToken(bearerToken);
        tokenBlacklistRepository.save(TokenBlacklist.builder()
            .token(token)
            .expiryTime(LocalDateTime.ofInstant(jwtService.extractExpiration(token), ZoneOffset.UTC))
            .build());

        String username = jwtService.extractUsername(token);
        userRepository.findByUsername(username)
            .ifPresent(user -> refreshTokenRepository.deleteByUserId(user.getId()));

        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = userRepository.save(User.builder()
            .username(request.getUsername())
            .fullName(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .enabled(true)
            .build());

        return DtoMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Email not found"));
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresInSeconds(jwtProperties.getAccessTokenMinutes() * 60)
            .build();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return bearerToken.substring(7);
    }
}

