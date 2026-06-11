package demo.project.service;

import demo.project.common.enums.BookingStatus;
import demo.project.dto.request.BookingCreateRequest;
import demo.project.dto.response.BookingResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    BookingResponse createBooking(String username, BookingCreateRequest request);
    List<BookingResponse> getMyBooking(String username);
    BookingResponse updateStatusByManager(Long id, BookingStatus status, String username);
    BookingResponse updateStatusByAdmin(Long id, BookingStatus status);
    List<BookingResponse> getBookingByDateAndStatus(LocalDate date, BookingStatus status);
    List<BookingResponse> getBookingByDateAndStatusForManager(LocalDate date, BookingStatus status, String username);
}
