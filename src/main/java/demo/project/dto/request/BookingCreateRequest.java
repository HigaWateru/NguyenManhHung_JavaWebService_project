package demo.project.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class BookingCreateRequest {
    @NotNull
    private Long courtId;

    @NotBlank
    @Pattern(
        regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$",
        message = "Khung giờ phải có dạng HH:mm-HH:mm"
    )
    private String timeSlot;

    @NotNull
    @FutureOrPresent
    private LocalDate bookingDate;

    private BigDecimal totalPrice;
}

