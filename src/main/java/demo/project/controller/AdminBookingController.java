package demo.project.controller;

import demo.project.common.enums.BookingStatus;
import demo.project.dto.request.BookingApprovalRequest;
import demo.project.dto.response.ApiResponse;
import demo.project.dto.response.BookingResponse;
import demo.project.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {
    private final BookingService bookingService;

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(@PathVariable Long bookingId,
                                                                     @Valid @RequestBody BookingApprovalRequest request) {
        BookingResponse response = bookingService.updateStatusByAdmin(bookingId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> byDateAndStatus(@RequestParam LocalDate date,
                                                                               @RequestParam BookingStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Fetched successfully",
            bookingService.getBookingByDateAndStatus(date, status)));
    }
}

