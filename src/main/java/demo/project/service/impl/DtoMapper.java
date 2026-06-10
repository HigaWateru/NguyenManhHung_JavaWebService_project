package demo.project.service.impl;

import demo.project.dto.response.BookingResponse;
import demo.project.dto.response.UserResponse;
import demo.project.entity.Booking;
import demo.project.entity.User;

public final class DtoMapper {
    private DtoMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder().id(user.getId()).username(user.getUsername()).fullName(user.getFullName())
            .email(user.getEmail()).phoneNumber(user.getPhoneNumber()).role(user.getRole()).enabled(user.isEnabled())
            .build();
    }

    public static BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
            .id(booking.getId()).courtId(booking.getCourt().getId()).courtName(booking.getCourt().getCourtName())
            .timeSlot(booking.getTimeSlot()).bookingDate(booking.getBookingDate()).totalPrice(booking.getTotalPrice())
            .status(booking.getStatus()).username(booking.getUser().getUsername()).createdAt(booking.getCreatedAt())
            .build();
    }
}

