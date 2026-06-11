package demo.project.controller;

import demo.project.common.enums.BookingStatus;
import demo.project.dto.request.BookingApprovalRequest;
import demo.project.dto.response.ApiResponse;
import demo.project.dto.response.BookingResponse;
import demo.project.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/bookings")
@RequiredArgsConstructor
public class ManagerBookingController {
    private final BookingService bookingService;

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(@PathVariable Long bookingId,
                Authentication authentication, @Valid @RequestBody BookingApprovalRequest request) {
        BookingResponse response = bookingService.updateStatusByManager(bookingId, request.getStatus(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> byDateAndStatus(@RequestParam LocalDate date,
            @RequestParam BookingStatus status, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Fetched successfully",
            bookingService.getBookingByDateAndStatusForManager(date, status, authentication.getName())));
    }
}
