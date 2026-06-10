package demo.project.seeder;

import demo.project.common.enums.BookingStatus;
import demo.project.common.enums.Role;
import demo.project.entity.BadmintonCluster;
import demo.project.entity.Booking;
import demo.project.entity.Court;
import demo.project.entity.User;
import demo.project.repository.BadmintonClusterRepository;
import demo.project.repository.BookingRepository;
import demo.project.repository.CourtRepository;
import demo.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final BadmintonClusterRepository clusterRepository;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0 || courtRepository.count() > 0 || bookingRepository.count() > 0) return;

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User admin = newUser("admin", "System Admin", "admin@badminton.local", Role.ROLE_ADMIN, passwordEncoder);
        User managerA = newUser("manager.a", "Manager Cluster A", "manager.a@badminton.local", Role.ROLE_MANAGER, passwordEncoder);
        User managerB = newUser("manager.b", "Manager Cluster B", "manager.b@badminton.local", Role.ROLE_MANAGER, passwordEncoder);
        User customerA = newUser("customer.a", "Customer A", "customer.a@badminton.local", Role.ROLE_CUSTOMER, passwordEncoder);
        User customerB = newUser("customer.b", "Customer B", "customer.b@badminton.local", Role.ROLE_CUSTOMER, passwordEncoder);

        userRepository.saveAll(List.of(admin, managerA, managerB, customerA, customerB));

        BadmintonCluster clusterA = BadmintonCluster.builder().name("SkyBird Arena").address("12 Nguyen Hue, District 1")
            .hotLine("0901000001").manager(managerA).build();

        BadmintonCluster clusterB = BadmintonCluster.builder().name("Phoenix Smash Center")
            .address("45 Le Loi, District 3").hotLine("0901000002").manager(managerB).build();

        clusterRepository.saveAll(List.of(clusterA, clusterB));

        Court courtA1 = newCourt("Court A1", "Indoor", true, clusterA);
        Court courtA2 = newCourt("Court A2", "Indoor", true, clusterA);
        Court courtB1 = newCourt("Court B1", "Premium", true, clusterB);
        Court courtB2 = newCourt("Court B2", "Premium", false, clusterB);

        courtRepository.saveAll(List.of(courtA1, courtA2, courtB1, courtB2));

        bookingRepository.saveAll(List.of(
            newBooking(customerA, courtA1, LocalDate.now().plusDays(1), "18:00-19:30", BookingStatus.CONFIRMED, 180000),
            newBooking(customerA, courtA2, LocalDate.now().plusDays(2), "19:30-21:00", BookingStatus.PENDING, 200000),
            newBooking(customerB, courtB1, LocalDate.now().minusDays(2), "17:00-18:30", BookingStatus.COMPLETED, 250000),
            newBooking(customerB, courtB2, LocalDate.now().plusDays(3), "20:00-21:30", BookingStatus.CANCELLED, 220000)
        ));
    }

    private User newUser(String username, String fullName, String email, Role role, BCryptPasswordEncoder passwordEncoder) {
        return User.builder().username(username).fullName(fullName).email(email).phoneNumber("0900000000")
            .password(passwordEncoder.encode("123456")).role(role).enabled(true).build();
    }

    private Court newCourt(String courtName, String type, boolean available, BadmintonCluster cluster) {
        return Court.builder().courtName(courtName).type(type).isAvailable(available).cluster(cluster).build();
    }

    private Booking newBooking(User user, Court court, LocalDate bookingDate, String timeSlot, BookingStatus status, long price) {
        return Booking.builder().user(user).court(court).bookingDate(bookingDate).timeSlot(timeSlot).status(status)
            .totalPrice(BigDecimal.valueOf(price)).build();
    }
}

