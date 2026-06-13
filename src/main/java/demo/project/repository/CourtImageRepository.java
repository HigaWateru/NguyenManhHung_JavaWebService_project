package demo.project.repository;

import demo.project.entity.CourtImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourtImageRepository extends JpaRepository<CourtImage, Long> {
    List<CourtImage> findByCourtId(Long courtId);

    Optional<CourtImage> findByIdAndCourtId(Long imageId, Long courtId);
}

