package demo.project.service.impl;

import demo.project.common.enums.BookingStatus;
import demo.project.dto.request.BookingCreateRequest;
import demo.project.dto.response.BookingResponse;
import demo.project.entity.Booking;
import demo.project.entity.Court;
import demo.project.entity.User;
import demo.project.exception.AppException;
import demo.project.repository.BookingRepository;
import demo.project.repository.CourtRepository;
import demo.project.repository.UserRepository;
import demo.project.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(String username, BookingCreateRequest request) {
        User user = userRepository.findByUsername(username).filter(User::isEnabled).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        Court court = courtRepository.findById(request.getCourtId()).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Court not found"));

        TimeRange requestedRange = parseTimeSlot(request.getTimeSlot());

        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

        boolean duplicated = bookingRepository
            .findByCourtIdAndBookingDateAndStatusIn(request.getCourtId(), request.getBookingDate(), activeStatuses)
            .stream().map(booking -> parseTimeSlot(booking.getTimeSlot()))
            .anyMatch(existing -> isOverlapped(requestedRange, existing));

        if (duplicated) throw new AppException(HttpStatus.CONFLICT, "Booking already exists");

        Booking booking = bookingRepository.save(Booking.builder().user(user).court(court).bookingDate(request.getBookingDate())
            .timeSlot(request.getTimeSlot()).totalPrice(request.getTotalPrice()).status(BookingStatus.PENDING).build());
        return DtoMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    public List<BookingResponse> getMyBooking(String username) {
        Long userId = userRepository.findByUsername(username).map(User::getId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return bookingRepository.findDetailedByUserId(userId).stream().map(DtoMapper::toBookingResponse).toList();
    }

    @Override
    @Transactional
    public BookingResponse updateStatusByManager(Long id, BookingStatus status, String username) {
        validateApprovalStatus(status);

        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));

        String ownerManager = booking.getCourt().getCluster() == null || booking.getCourt().getCluster().getManager() == null
            ? null
            : booking.getCourt().getCluster().getManager().getUsername();

        if (ownerManager == null || !ownerManager.equals(username)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not allowed to process this booking");
        }

        ensurePendingBeforeProcessing(booking);
        booking.setStatus(status);
        return DtoMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updateStatusByAdmin(Long id, BookingStatus status) {
        validateApprovalStatus(status);

        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));

        ensurePendingBeforeProcessing(booking);
        booking.setStatus(status);
        return DtoMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    public List<BookingResponse> getBookingByDateAndStatus(LocalDate date, BookingStatus status) {
        return bookingRepository.findByBookingDateAndStatus(date, status).stream().map(DtoMapper::toBookingResponse).toList();
    }

    @Override
    public List<BookingResponse> getBookingByDateAndStatusForManager(LocalDate date, BookingStatus status, String username) {
        return bookingRepository.findByBookingDateAndStatusAndCourtClusterManagerUsername(date, status, username)
            .stream().map(DtoMapper::toBookingResponse).toList();
    }

    private static void validateApprovalStatus(BookingStatus status) {
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.CANCELLED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    private static void ensurePendingBeforeProcessing(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT, "Only pending booking can be approved or rejected");
        }
    }

    private static TimeRange parseTimeSlot(String timeSlot) {
        String[] parts = timeSlot.split("-");
        if (parts.length != 2) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khung gio khong hop le");
        }

        try {
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);

            if (!start.isBefore(end)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Gio bat dau phai nho hon gio ket thuc");
            }
            return new TimeRange(start, end);
        } catch (DateTimeParseException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khung gio khong hop le");
        }
    }

    private static boolean isOverlapped(TimeRange requested, TimeRange existing) {
        return requested.start().isBefore(existing.end()) && requested.end().isAfter(existing.start());
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
