package demo.project.security.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenBlacklistService {
    private static final String BLACKLIST_PREFIX = "bl:access:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public RedisTokenBlacklistService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void blacklistAccessToken(String token) {
        long remainingSeconds = jwtService.getRemainingSeconds(token);
        if (remainingSeconds <= 0) {
            return;
        }

        String tokenId = jwtService.resolveTokenId(token);
        redisTemplate.opsForValue().set(buildKey(tokenId), "1", Duration.ofSeconds(remainingSeconds));
    }

    public boolean isAccessTokenBlacklisted(String token) {
        String tokenId = jwtService.resolveTokenId(token);
        Boolean exists = redisTemplate.hasKey(buildKey(tokenId));
        return Boolean.TRUE.equals(exists);
    }

    private String buildKey(String tokenId) {
        return BLACKLIST_PREFIX + tokenId;
    }
}

