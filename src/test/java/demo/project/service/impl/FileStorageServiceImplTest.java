package demo.project.service.impl;

import com.cloudinary.Cloudinary;
import demo.project.entity.BadmintonCluster;
import demo.project.entity.Court;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.CourtRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {
    @Mock
    private Cloudinary cloudinary;

    @Mock
    private CourtRepository courtRepository;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @Test
    void uploadCourtImageShouldRejectUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        AppException ex = assertThrows(AppException.class,
            () -> fileStorageService.uploadCourtImage(1L, file, "manager.a", false));

        assertEquals(400, ex.getStatus().value());
        assertEquals("Only image file types are allowed (jpeg, png, webp, gif)", ex.getMessage());
    }

    @Test
    void uploadCourtImageShouldRejectFileLargerThan50Mb() {
        byte[] content = new byte[(50 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", content);

        AppException ex = assertThrows(AppException.class,
            () -> fileStorageService.uploadCourtImage(1L, file, "manager.a", false));

        assertEquals(400, ex.getStatus().value());
        assertEquals("File size must be less than or equal to 50MB", ex.getMessage());
    }

    @Test
    void uploadCourtImageShouldRejectImageDimensionOverLimit() throws IOException {
        byte[] imageBytes = createPng(5100, 200);
        MockMultipartFile file = new MockMultipartFile("file", "large-dim.png", "image/png", imageBytes);

        AppException ex = assertThrows(AppException.class,
            () -> fileStorageService.uploadCourtImage(1L, file, "manager.a", false));

        assertEquals(400, ex.getStatus().value());
        assertEquals("Image dimensions exceed allowed limit: max 5000x5000 pixels", ex.getMessage());
    }

    @Test
    void uploadCourtImageShouldRejectManagerWhenCourtIsNotOwned() throws IOException {
        byte[] imageBytes = createPng(300, 300);
        MockMultipartFile file = new MockMultipartFile("file", "ok.png", "image/png", imageBytes);

        User owner = User.builder().username("manager.a").build();
        BadmintonCluster cluster = BadmintonCluster.builder().manager(owner).build();
        Court court = Court.builder().id(1L).cluster(cluster).build();

        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        AppException ex = assertThrows(AppException.class,
            () -> fileStorageService.uploadCourtImage(1L, file, "manager.b", false));

        assertEquals(403, ex.getStatus().value());
        assertEquals("You are not allowed to upload image for this court", ex.getMessage());
    }

    private static byte[] createPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}

