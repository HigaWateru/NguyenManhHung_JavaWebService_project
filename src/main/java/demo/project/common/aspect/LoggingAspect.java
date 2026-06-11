package demo.project.common.aspect;

import demo.project.dto.response.BookingResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @AfterReturning(
        pointcut = "execution(* demo.project.service.impl.BookingServiceImpl.createBooking(..))",
        returning = "result"
    )
    public void logBookingSuccess(JoinPoint joinPoint, Object result) {
        if (result instanceof BookingResponse booking) {
            log.info("[AUDIT - SUCCESS] Customer {} booked court {} on {} at slot {}",booking.getUsername(),
                booking.getCourtName(),booking.getBookingDate(),booking.getTimeSlot());
        }
    }

    @AfterThrowing(
        pointcut = "execution(* demo.project.service.impl.BookingServiceImpl.createBooking(..))",
        throwing = "ex"
    )
    public void logBookingFailure(JoinPoint joinPoint, Throwable ex) {
        Object[] args = joinPoint.getArgs();
        String username = args.length > 0 ? String.valueOf(args[0]) : "unknown";
        log.warn("[AUDIT - FAILED] Customer {} booking failed due to: {}", username, ex.getMessage());
    }
}

