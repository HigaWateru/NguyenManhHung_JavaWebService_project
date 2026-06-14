package demo.project.service.impl;

import demo.project.entity.PasswordResetOtp;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.PasswordResetOtpRepository;
import demo.project.repository.RefreshTokenRepository;
import demo.project.repository.UserRepository;
import demo.project.security.jwt.JwtProperties;
import demo.project.security.jwt.RedisTokenBlacklistService;
import demo.project.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplForgotPasswordTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RedisTokenBlacklistService redisTokenBlacklistService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void forgotPasswordShouldGenerateOtpAndSendMail() {
        User user = User.builder().id(10L).email("user@test.local").build();
        when(userRepository.findByEmail("user@test.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");

        authService.forgotPassword("user@test.local");

        ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(passwordResetOtpRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        verify(passwordResetOtpRepository).deleteByUserId(10L);
        verify(passwordResetOtpRepository).save(otpCaptor.capture());
        verify(mailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));

        PasswordResetOtp savedOtp = otpCaptor.getValue();
        assertNotNull(savedOtp);
        assertEquals("encoded-otp", savedOtp.getOtpHash());
        assertEquals(user, savedOtp.getUser());
    }

    @Test
    void verifyForgotPasswordOtpShouldResetPasswordWhenOtpIsValid() {
        User user = User.builder().id(20L).email("valid@test.local").password("old-password").build();
        PasswordResetOtp otpEntity = PasswordResetOtp.builder().user(user).otpHash("hashed-otp")
            .expiresAt(LocalDateTime.now().plusMinutes(3)).verified(false).failedAttempts(0).createdAt(LocalDateTime.now())
            .build();

        when(userRepository.findByEmail("valid@test.local")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.findTopByUserIdAndVerifiedFalseOrderByCreatedAtDesc(20L)).thenReturn(Optional.of(otpEntity));
        when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0, String.class));

        String newPassword = authService.verifyForgotPasswordOtp("valid@test.local", "123456");

        assertEquals("123456", newPassword);
        verify(userRepository).save(user);
        verify(passwordResetOtpRepository, times(1)).save(otpEntity);
        assertTrue(otpEntity.isVerified());
    }

    @Test
    void verifyForgotPasswordOtpShouldIncreaseFailedAttemptsWhenOtpInvalid() {
        User user = User.builder().id(30L).email("invalid@test.local").build();
        PasswordResetOtp otpEntity = PasswordResetOtp.builder().user(user).otpHash("hashed-otp")
            .expiresAt(LocalDateTime.now().plusMinutes(3)).verified(false).failedAttempts(0).createdAt(LocalDateTime.now())
            .build();

        when(userRepository.findByEmail("invalid@test.local")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.findTopByUserIdAndVerifiedFalseOrderByCreatedAtDesc(30L)).thenReturn(Optional.of(otpEntity));
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
            () -> authService.verifyForgotPasswordOtp("invalid@test.local", "000000"));

        assertEquals(400, ex.getStatus().value());
        assertEquals("Invalid OTP", ex.getMessage());
        assertEquals(1, otpEntity.getFailedAttempts());
        verify(passwordResetOtpRepository).save(otpEntity);
    }
}


