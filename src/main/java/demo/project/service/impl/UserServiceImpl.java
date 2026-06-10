package demo.project.service.impl;

import demo.project.dto.request.UserUpsertRequest;
import demo.project.dto.response.PageResponse;
import demo.project.dto.response.UserResponse;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.UserRepository;
import demo.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, int page, int size) {
        Page<User> users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            keyword == null ? "" : keyword,
            keyword == null ? "" : keyword,
            PageRequest.of(page, size)
        );

        return PageResponse.<UserResponse>builder().content(users.getContent().stream().map(DtoMapper::toUserResponse).toList())
            .page(users.getNumber()).size(users.getSize()).totalElements(users.getTotalElements())
            .totalPages(users.getTotalPages()).build();
    }

    @Override
    @Transactional
    public UserResponse create(UserUpsertRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email already exists");
        }

        String password = request.getPassword() == null ? "123456" : request.getPassword();

        User user = userRepository.save(User.builder().username(request.getUsername()).fullName(request.getUsername())
            .email(request.getEmail()).password(passwordEncoder.encode(password)).role(request.getRole())
            .enabled(request.getEnabled()).build());

        return DtoMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpsertRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        user.setUsername(request.getUsername());
        user.setFullName(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return DtoMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) throw new AppException(HttpStatus.NOT_FOUND, "User not found");
        userRepository.deleteById(id);
    }
}

