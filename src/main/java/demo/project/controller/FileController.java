package demo.project.controller;

import demo.project.dto.response.ApiResponse;
import demo.project.dto.response.CourtImageResponse;
import demo.project.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping("/courts/{courtId}/images")
    public ResponseEntity<ApiResponse<CourtImageResponse>> addCourtImage(@PathVariable Long courtId, Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {
        CourtImageResponse response = fileStorageService.addCourtImage(courtId, file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Court image added successfully", response));
    }

    @GetMapping("/courts/{courtId}/images")
    public ResponseEntity<ApiResponse<List<CourtImageResponse>>> getCourtImages(@PathVariable Long courtId, Authentication authentication) {
        List<CourtImageResponse> response = fileStorageService.getCourtImages(courtId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Court images fetched successfully", response));
    }

    @PutMapping("/courts/{courtId}/images/{imageId}")
    public ResponseEntity<ApiResponse<CourtImageResponse>> updateCourtImage(@PathVariable Long courtId, @PathVariable Long imageId,
            Authentication authentication, @RequestParam("file") MultipartFile file) throws IOException {
        CourtImageResponse response = fileStorageService.updateCourtImage(courtId, imageId, file, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Court image updated successfully", response));
    }

    @DeleteMapping("/courts/{courtId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourtImage(@PathVariable Long courtId, @PathVariable Long imageId,
            Authentication authentication) throws IOException {
        fileStorageService.deleteCourtImage(courtId, imageId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Court image deleted successfully", null));
    }
}
