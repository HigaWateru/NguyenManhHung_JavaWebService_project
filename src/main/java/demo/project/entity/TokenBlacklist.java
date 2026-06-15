package demo.project.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash("tokenBlacklist")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
public class TokenBlacklist {
    @Id
    private String tokenId;

    private String token;

    @TimeToLive
    private Long ttlSeconds;
}