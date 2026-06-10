package demo.project.dto.request;

import demo.project.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserUpsertRequest {
    @NotBlank
    @Size(min = 4, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @Size(min = 6, max = 255)
    private String password;

    @NotNull
    private Role role;

    @NotNull
    private Boolean enabled;
}

