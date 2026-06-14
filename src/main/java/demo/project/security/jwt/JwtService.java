package demo.project.security.jwt;

import demo.project.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    public String generateAccessToken(String username, Role role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder().id(UUID.randomUUID().toString()).subject(username).claims(Map.of("role", role.name())).issuedAt(Date.from(now))
            .expiration(Date.from(expiration)).signWith(getSigningKey()).compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getRefreshTokenDays(), ChronoUnit.DAYS);

        return Jwts.builder().subject(username).issuedAt(Date.from(now)).expiration(Date.from(expiration))
            .signWith(getSigningKey()).compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Instant extractExpiration(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }

    public long getRemainingSeconds(String token) {
        long seconds = ChronoUnit.SECONDS.between(Instant.now(), extractExpiration(token));
        return Math.max(seconds, 0);
    }

    public String resolveTokenId(String token) {
        String jti = extractAllClaims(token).getId();
        if (jti != null && !jti.isBlank()) {
            return jti;
        }
        return sha256Hex(token);
    }

    public boolean isTokenValid(String token, String username) {
        String tokenUsername = extractUsername(token);
        return tokenUsername.equals(username) && extractExpiration(token).isAfter(Instant.now());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = resolveSecretBytes(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveSecretBytes(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            try {
                return Decoders.BASE64URL.decode(secret);
            } catch (IllegalArgumentException ignoredAgain) {
                // Fall back to using the configured secret as plain text.
                return secret.getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
