package demo.project.dto.response;

import demo.project.common.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter @Setter @Builder
public class BookingResponse {
    private final Long id;
    private final Long courtId;
    private final String courtName;
    private final String timeSlot;
    private final LocalDate bookingDate;
    private final BigDecimal totalPrice;
    private final BookingStatus status;
    private final String username;
    private final LocalDateTime createdAt;
}

