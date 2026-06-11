package demo.project.controller;

import demo.project.dto.request.BookingCreateRequest;
import demo.project.dto.response.ApiResponse;
import demo.project.dto.response.BookingResponse;
import demo.project.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
public class CustomerBookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(Authentication authentication,
            @Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = bookingService.createBooking(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Fetched successfully", bookingService.getMyBooking(authentication.getName())));
    }
}

