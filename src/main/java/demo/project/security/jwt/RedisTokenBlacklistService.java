package demo.project.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService {
    private static final String BLACKLIST_PREFIX = "bl:access:";
    private static final Logger log = LoggerFactory.getLogger(RedisTokenBlacklistService.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void blacklistAccessToken(String token) {
        long remainingSeconds = jwtService.getRemainingSeconds(token);
        if (remainingSeconds <= 0) {
            return;
        }

        String tokenId = jwtService.resolveTokenId(token);
        try {
            redisTemplate.opsForValue().set(buildKey(tokenId), "1", Duration.ofSeconds(remainingSeconds));
        } catch (DataAccessException ex) {
            // Do not fail logout when Redis is temporarily unavailable.
            log.warn("Skip blacklisting token because Redis is unavailable: {}", ex.getMessage());
        }
    }

    public boolean isAccessTokenBlacklisted(String token) {
        String tokenId = jwtService.resolveTokenId(token);
        try {
            Boolean exists = redisTemplate.hasKey(buildKey(tokenId));
            return Boolean.TRUE.equals(exists);
        } catch (DataAccessException ex) {
            // Keep APIs available when Redis is down.
            log.warn("Cannot verify token blacklist because Redis is unavailable: {}", ex.getMessage());
            return false;
        }
    }

    private String buildKey(String tokenId) {
        return BLACKLIST_PREFIX + tokenId;
    }
}

