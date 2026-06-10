package demo.project.service;

import demo.project.dto.request.ChangePasswordRequest;
import demo.project.dto.request.LoginRequest;
import demo.project.dto.request.RefreshTokenRequest;
import demo.project.dto.request.RegisterRequest;
import demo.project.dto.response.AuthResponse;
import demo.project.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String bearerToken);
    UserResponse register(RegisterRequest request);
    void changePassword(String username, ChangePasswordRequest request);
    void forgotPassword(String email);
}

