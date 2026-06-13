package demo.project.service.impl;

import com.cloudinary.Cloudinary;
import demo.project.dto.response.CourtImageResponse;
import demo.project.entity.BadmintonCluster;
import demo.project.entity.Court;
import demo.project.entity.CourtImage;
import demo.project.exception.AppException;
import demo.project.repository.CourtImageRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final int MAX_IMAGE_WIDTH = 5000;
    private static final int MAX_IMAGE_HEIGHT = 5000;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;
    private final CourtRepository courtRepository;
    private final CourtImageRepository courtImageRepository;

    @Override
    @Transactional
    public CourtImageResponse addCourtImage(Long courtId, MultipartFile file, String username) throws IOException {
        validateFile(file);

        Court court = findCourtById(courtId);
        validateUploadPermission(court, username);

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of());
        String imageUrl = String.valueOf(result.get("secure_url"));
        String publicId = String.valueOf(result.get("public_id"));

        CourtImage courtImage = courtImageRepository.save(CourtImage.builder().court(court).imageUrl(imageUrl).publicId(publicId).build());
        return toResponse(courtImage);
    }

    @Override
    @Transactional
    public CourtImageResponse updateCourtImage(Long courtId, Long imageId, MultipartFile file, String username) throws IOException {
        validateFile(file);

        Court court = findCourtById(courtId);
        validateUploadPermission(court, username);

        CourtImage courtImage = courtImageRepository.findByIdAndCourtId(imageId, courtId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Court image not found"));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of());
        String imageUrl = String.valueOf(result.get("secure_url"));
        String publicId = String.valueOf(result.get("public_id"));

        String oldPublicId = courtImage.getPublicId();
        courtImage.setImageUrl(imageUrl);
        courtImage.setPublicId(publicId);

        CourtImage savedImage = courtImageRepository.save(courtImage);
        safeDeleteFromCloudinary(oldPublicId);
        return toResponse(savedImage);
    }

    @Override
    @Transactional
    public void deleteCourtImage(Long courtId, Long imageId, String username) throws IOException {
        Court court = findCourtById(courtId);
        validateUploadPermission(court, username);

        CourtImage courtImage = courtImageRepository.findByIdAndCourtId(imageId, courtId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Court image not found"));

        courtImageRepository.delete(courtImage);
        safeDeleteFromCloudinary(courtImage.getPublicId());
    }

    @Override
    @Transactional
    public List<CourtImageResponse> getCourtImages(Long courtId, String username) {
        Court court = findCourtById(courtId);
        validateUploadPermission(court, username);

        return courtImageRepository.findByCourtId(courtId).stream().map(FileStorageServiceImpl::toResponse).toList();
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

    private static void validateUploadPermission(Court court, String username) {
        BadmintonCluster cluster = court.getCluster();
        String managerUsername = cluster == null || cluster.getManager() == null ? null : cluster.getManager().getUsername();

        if (managerUsername == null || !managerUsername.equals(username)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not allowed to manage images for this court");
        }
    }

    private static CourtImageResponse toResponse(CourtImage image) {
        return CourtImageResponse.builder().id(image.getId()).courtId(image.getCourt().getId()).secureUrl(image.getImageUrl())
            .createdAt(image.getCreatedAt()).updatedAt(image.getUpdatedAt()).build();
    }

    private Court findCourtById(Long courtId) {
        return courtRepository.findById(courtId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Court not found"));
    }

    private void safeDeleteFromCloudinary(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, Map.of());
        } catch (Exception ignored) {
            // Keep DB state consistent even if cloud cleanup fails unexpectedly.
        }
    }
}
