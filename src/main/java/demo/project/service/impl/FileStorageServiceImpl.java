package demo.project.service.impl;

import com.cloudinary.Cloudinary;
import demo.project.entity.BadmintonCluster;
import demo.project.entity.Court;
import demo.project.exception.AppException;
import demo.project.repository.CourtRepository;
import demo.project.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final int MAX_IMAGE_WIDTH = 5000;
    private static final int MAX_IMAGE_HEIGHT = 5000;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;
    private final CourtRepository courtRepository;

    @Override
    @Transactional
    public String uploadCourtImage(Long courtId, MultipartFile file, String username, boolean isAdmin) throws IOException {
        validateFile(file);

        Court court = courtRepository.findById(courtId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Court not found"));
        validateUploadPermission(court, username, isAdmin);

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of());
        String imageUrl = String.valueOf(result.get("secure_url"));
        court.setImage(imageUrl);
        courtRepository.save(court);
        return imageUrl;
    }

    private static void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Only image file types are allowed (jpeg, png, webp, gif)");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File size must be less than or equal to 50MB");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Invalid image file");
            }
            if (image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                    "Image dimensions exceed allowed limit: max 5000x5000 pixels");
            }
        }
    }

    private static void validateUploadPermission(Court court, String username, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        BadmintonCluster cluster = court.getCluster();
        String managerUsername = cluster == null || cluster.getManager() == null ? null : cluster.getManager().getUsername();

        if (managerUsername == null || !managerUsername.equals(username)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not allowed to upload image for this court");
        }
    }
}
