package demo.project.service;

import demo.project.dto.response.CourtImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileStorageService {
    CourtImageResponse addCourtImage(Long courtId, MultipartFile file, String username) throws IOException;

    CourtImageResponse updateCourtImage(Long courtId, Long imageId, MultipartFile file, String username) throws IOException;

    void deleteCourtImage(Long courtId, Long imageId, String username) throws IOException;

    List<CourtImageResponse> getCourtImages(Long courtId, String username);
}
