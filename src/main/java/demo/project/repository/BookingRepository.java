package demo.project.repository;

import demo.project.common.enums.BookingStatus;
import demo.project.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select b from Booking b join fetch b.court join fetch b.user where b.user.id = :userId order by b.createdAt desc")
    List<Booking> findDetailedByUserId(Long userId);

    List<Booking> findByBookingDate(LocalDate bookingDate);
    List<Booking> findByBookingDateAndStatus(LocalDate bookingDate, BookingStatus status);

    @Query("select b from Booking b join fetch b.court join fetch b.user where b.id = :bookingId")
    Optional<Booking> findDetailedById(Long bookingId);

    boolean existsByCourtIdAndBookingDateAndTimeSlotAndStatusIn(Long courtId, LocalDate bookingDate, String timeSlot, List<BookingStatus> statuses);
}

