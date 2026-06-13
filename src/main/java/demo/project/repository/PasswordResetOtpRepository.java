package demo.project.repository;

import demo.project.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository <PasswordResetOtp, Long> {
    void deleteByUserId(Long userId);

    Optional<PasswordResetOtp> findTopByUserIdAndVerifiedFalseOrderByCreatedAtDesc(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}

