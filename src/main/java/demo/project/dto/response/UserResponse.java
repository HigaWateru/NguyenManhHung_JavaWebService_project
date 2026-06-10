package demo.project.dto.response;

import demo.project.common.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private final Long id;
    private final String username;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final Role role;
    private final boolean enabled;
}

