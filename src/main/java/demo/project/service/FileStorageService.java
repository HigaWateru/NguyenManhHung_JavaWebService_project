package demo.project.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String uploadCourtImage(Long courtId, MultipartFile file, String username, boolean isAdmin) throws IOException;
}
