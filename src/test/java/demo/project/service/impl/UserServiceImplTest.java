package demo.project.service.impl;

import demo.project.common.enums.Role;
import demo.project.dto.request.UserUpsertRequest;
import demo.project.dto.response.PageResponse;
import demo.project.dto.response.UserResponse;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void searchUsersShouldMapPageResult() {
        User user = User.builder().id(1L).username("admin.root").fullName("Admin Root")
            .email("admin.root@badminton.local").phoneNumber("0901100001").role(Role.ROLE_ADMIN).enabled(true)
            .build();

        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("", "", PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));

        PageResponse<UserResponse> result = userService.searchUsers(null, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("admin.root", result.getContent().getFirst().getUsername());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void createShouldThrowConflictWhenUsernameExists() {
        UserUpsertRequest request = request("manager.a", "manager.a@badminton.local", "MngA@2026!", Role.ROLE_MANAGER);
        when(userRepository.existsByUsername("manager.a")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.create(request));

        assertEquals(409, ex.getStatus().value());
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void createShouldSaveEncodedPassword() {
        UserUpsertRequest request = request("customer.g", "customer.g@badminton.local", "CusG@2026#", Role.ROLE_CUSTOMER);

        when(userRepository.existsByUsername("customer.g")).thenReturn(false);
        when(userRepository.existsByEmail("customer.g@badminton.local")).thenReturn(false);
        when(passwordEncoder.encode("CusG@2026#")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(100L);
            return saved;
        });

        UserResponse response = userService.create(request);

        assertEquals(100L, response.getId());
        assertEquals("customer.g", response.getUsername());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
    }

    @Test
    void updateShouldApplyChangesAndEncodePassword() {
        User existing = User.builder().id(20L).username("old.name").fullName("Old Name")
            .email("old@badminton.local").password("old-encoded").role(Role.ROLE_CUSTOMER).enabled(false)
            .build();

        UserUpsertRequest request = request("manager.updated", "manager.updated@badminton.local", "MngUpdated@2026", Role.ROLE_MANAGER);
        request.setEnabled(true);

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("MngUpdated@2026")).thenReturn("new-encoded");
        when(userRepository.save(existing)).thenReturn(existing);

        UserResponse response = userService.update(20L, request);

        assertEquals("manager.updated", response.getUsername());
        assertEquals(Role.ROLE_MANAGER, response.getRole());
        assertTrue(response.isEnabled());
        assertEquals("new-encoded", existing.getPassword());
        verify(passwordEncoder).encode("MngUpdated@2026");
    }

    @Test
    void deleteShouldThrowNotFoundWhenUserMissing() {
        when(userRepository.existsById(404L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> userService.delete(404L));

        assertEquals(404, ex.getStatus().value());
        assertEquals("User not found", ex.getMessage());
    }

    private static UserUpsertRequest request(String username, String email, String password, Role role) {
        UserUpsertRequest request = new UserUpsertRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        request.setEnabled(true);
        return request;
    }
}


