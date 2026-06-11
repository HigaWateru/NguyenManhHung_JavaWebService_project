package demo.project.service.impl;

import com.cloudinary.Cloudinary;
import demo.project.entity.Court;
import demo.project.exception.AppException;
import demo.project.repository.CourtRepository;
import demo.project.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private final Cloudinary cloudinary;
    private final CourtRepository courtRepository;

    @Override
    @Transactional
    public String uploadCourtImage(Long courtId, MultipartFile file) throws IOException {
        if(file == null || file.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "File is required");
        Court court = courtRepository.findById(courtId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Court not found"));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of());
        String imageUrl = String.valueOf(result.get("secure_url"));
        court.setImage(imageUrl);
        courtRepository.save(court);
        return imageUrl;
    }
}
