package demo.project.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class CourtImageResponse {
    private final Long id;
    private final Long courtId;
    private final String secureUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}

