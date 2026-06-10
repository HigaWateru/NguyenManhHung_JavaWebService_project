package demo.project.service;

import demo.project.dto.request.UserUpsertRequest;
import demo.project.dto.response.PageResponse;
import demo.project.dto.response.UserResponse;

public interface UserService {
    PageResponse<UserResponse> searchUsers(String keyword, int page, int size);
    UserResponse create(UserUpsertRequest request);
    UserResponse update(Long id, UserUpsertRequest request);
    void delete(Long id);
}

