package demo.project.security.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtService jwtService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void isAccessTokenBlacklistedShouldReturnFalseWhenRedisUnavailable() {
        RedisTokenBlacklistService service = new RedisTokenBlacklistService(redisTemplate, jwtService);
        when(jwtService.resolveTokenId("access-token")).thenReturn("token-id");
        when(redisTemplate.hasKey("bl:access:token-id"))
            .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

        boolean blacklisted = service.isAccessTokenBlacklisted("access-token");

        assertFalse(blacklisted);
    }

    @Test
    void blacklistAccessTokenShouldNotThrowWhenRedisUnavailable() {
        RedisTokenBlacklistService service = new RedisTokenBlacklistService(redisTemplate, jwtService);
        when(jwtService.getRemainingSeconds("access-token")).thenReturn(60L);
        when(jwtService.resolveTokenId("access-token")).thenReturn("token-id");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Unable to connect to Redis"))
            .when(valueOperations).set("bl:access:token-id", "1", Duration.ofSeconds(60));

        assertDoesNotThrow(() -> service.blacklistAccessToken("access-token"));
        verify(redisTemplate).opsForValue();
    }
}

