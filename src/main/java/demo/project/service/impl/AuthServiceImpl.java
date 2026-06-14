package demo.project.service.impl;

import demo.project.dto.request.ChangePasswordRequest;
import demo.project.dto.request.LoginRequest;
import demo.project.dto.request.RefreshTokenRequest;
import demo.project.dto.request.RegisterRequest;
import demo.project.dto.response.AuthResponse;
import demo.project.dto.response.UserResponse;
import demo.project.entity.PasswordResetOtp;
import demo.project.entity.RefreshToken;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.PasswordResetOtpRepository;
import demo.project.repository.RefreshTokenRepository;
import demo.project.repository.UserRepository;
import demo.project.security.jwt.JwtProperties;
import demo.project.security.jwt.RedisTokenBlacklistService;
import demo.project.security.jwt.JwtService;
import demo.project.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRE_MINUTES = 5;
    private static final int MAX_OTP_FAILED_ATTEMPTS = 5;
    private static final String DEFAULT_RESET_PASSWORD = "123456";

    private static final String OTP_ALPHABET = "0123456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTokenBlacklistService redisTokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername()).filter(User::isEnabled)
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder().token(refreshToken).user(user)
            .expiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(jwtProperties.getRefreshTokenDays() * 86400), ZoneOffset.UTC))
            .revoked(false).build());

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
        redisTokenBlacklistService.blacklistAccessToken(token);

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

        User user = userRepository.save(User.builder().username(request.getUsername())
            .fullName(request.getUsername()).email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword())).role(request.getRole())
            .enabled(true).build());

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
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Email not found"));

        LocalDateTime now = LocalDateTime.now();
        passwordResetOtpRepository.deleteByExpiresAtBefore(now);
        passwordResetOtpRepository.deleteByUserId(user.getId());

        String otp = generateRandomValue(OTP_ALPHABET, OTP_LENGTH);

        PasswordResetOtp otpEntity = PasswordResetOtp.builder().user(user).otpHash(passwordEncoder.encode(otp))
            .expiresAt(now.plusMinutes(OTP_EXPIRE_MINUTES)).verified(false).failedAttempts(0).createdAt(now).build();
        passwordResetOtpRepository.save(otpEntity);

        sendForgotPasswordOtpMail(user.getEmail(), otp);
    }

    @Override
    @Transactional
    public String verifyForgotPasswordOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Email not found"));

        PasswordResetOtp latestOtp = passwordResetOtpRepository.findTopByUserIdAndVerifiedFalseOrderByCreatedAtDesc(user.getId())
            .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "OTP not found. Please request a new OTP"));

        if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP expired. Please request a new OTP");
        }

        if (latestOtp.getFailedAttempts() >= MAX_OTP_FAILED_ATTEMPTS) {
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP has been locked due to too many failed attempts");
        }

        if (!passwordEncoder.matches(otp, latestOtp.getOtpHash())) {
            latestOtp.setFailedAttempts(latestOtp.getFailedAttempts() + 1);
            passwordResetOtpRepository.save(latestOtp);
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
        userRepository.save(user);

        latestOtp.setVerified(true);
        latestOtp.setVerifiedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(latestOtp);

        return DEFAULT_RESET_PASSWORD;
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
            .tokenType("Bearer").expiresInSeconds(jwtProperties.getAccessTokenMinutes() * 60)
            .build();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return bearerToken.substring(7);
    }

    private void sendForgotPasswordOtpMail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Ma OTP khoi phuc mat khau");
            message.setText("Ma OTP cua ban la: " + otp + "\nMa co hieu luc trong " + OTP_EXPIRE_MINUTES + " phut.");
            mailSender.send(message);
        } catch (MailException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to send OTP email. Please try again later");
        }
    }

    private static String generateRandomValue(String alphabet, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(alphabet.length());
            builder.append(alphabet.charAt(index));
        }
        return builder.toString();
    }
}

