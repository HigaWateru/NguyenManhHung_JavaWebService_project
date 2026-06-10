package demo.project.dto.request;

import demo.project.common.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingApprovalRequest {
    @NotNull
    private BookingStatus status;
}

