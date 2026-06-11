package demo.project.controller;

import demo.project.dto.response.ApiResponse;
import demo.project.dto.response.FileUploadResponse;
import demo.project.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping("/upload/court/{courtId}")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCourtImage(@PathVariable Long courtId, Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        String secureUrl = fileStorageService.uploadCourtImage(courtId, file, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", FileUploadResponse.builder().secureUrl(secureUrl).build()));
    }
}
