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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, User> users = new LinkedHashMap<>();
        users.put("admin.root", newUser("admin.root", "Admin Root", "admin.root@badminton.local", "0901100001", Role.ROLE_ADMIN, "adminroot123", passwordEncoder));
        users.put("admin.ops", newUser("admin.ops", "Admin Operations", "admin.ops@badminton.local", "0901100002", Role.ROLE_ADMIN, "adminops123", passwordEncoder));
        users.put("manager.a", newUser("manager.a", "Manager Cluster A", "manager.a@badminton.local", "0902100001", Role.ROLE_MANAGER, "managera123", passwordEncoder));
        users.put("manager.b", newUser("manager.b", "Manager Cluster B", "manager.b@badminton.local", "0902100002", Role.ROLE_MANAGER, "managerb123", passwordEncoder));
        users.put("manager.c", newUser("manager.c", "Manager Cluster C", "manager.c@badminton.local", "0902100003", Role.ROLE_MANAGER, "managerc123", passwordEncoder));
        users.put("manager.d", newUser("manager.d", "Manager Cluster D", "manager.d@badminton.local", "0902100004", Role.ROLE_MANAGER, "managerd123", passwordEncoder));
        users.put("customer.a", newUser("customer.a", "Customer A", "customer.a@badminton.local", "0903100001", Role.ROLE_CUSTOMER, "customera123", passwordEncoder));
        users.put("customer.b", newUser("customer.b", "Customer B", "customer.b@badminton.local", "0903100002", Role.ROLE_CUSTOMER, "customerb123", passwordEncoder));
        users.put("customer.c", newUser("customer.c", "Customer C", "customer.c@badminton.local", "0903100003", Role.ROLE_CUSTOMER, "customerc123", passwordEncoder));
        users.put("customer.d", newUser("customer.d", "Customer D", "customer.d@badminton.local", "0903100004", Role.ROLE_CUSTOMER, "customerd123", passwordEncoder));
        users.put("customer.e", newUser("customer.e", "Customer E", "customer.e@badminton.local", "0903100005", Role.ROLE_CUSTOMER, "customere123", passwordEncoder));
        users.put("customer.f", newUser("customer.f", "Customer F", "customer.f@badminton.local", "0903100006", Role.ROLE_CUSTOMER, "customerf123", passwordEncoder));
        userRepository.saveAll(users.values());

        BadmintonCluster clusterA = BadmintonCluster.builder().name("SkyBird Arena").address("12 Nguyen Hue, District 1")
            .hotLine("0901000001").manager(users.get("manager.a")).build();
        BadmintonCluster clusterB = BadmintonCluster.builder().name("Phoenix Smash Center")
            .address("45 Le Loi, District 3").hotLine("0901000002").manager(users.get("manager.b")).build();
        BadmintonCluster clusterC = BadmintonCluster.builder().name("Dragon Shuttle Hub")
            .address("88 Cach Mang Thang 8, District 10").hotLine("0901000003").manager(users.get("manager.c")).build();
        BadmintonCluster clusterD = BadmintonCluster.builder().name("Riverside Badminton House")
            .address("120 Vo Van Kiet, District 5").hotLine("0901000004").manager(users.get("manager.d")).build();
        clusterRepository.saveAll(List.of(clusterA, clusterB, clusterC, clusterD));

        Map<String, Court> courts = new LinkedHashMap<>();
        courts.put("A1", newCourt("Court A1", "Indoor", true, clusterA));
        courts.put("A2", newCourt("Court A2", "Indoor", true, clusterA));
        courts.put("A3", newCourt("Court A3", "Training", true, clusterA));
        courts.put("B1", newCourt("Court B1", "Premium", true, clusterB));
        courts.put("B2", newCourt("Court B2", "Premium", false, clusterB));
        courts.put("B3", newCourt("Court B3", "Indoor", true, clusterB));
        courts.put("C1", newCourt("Court C1", "Standard", true, clusterC));
        courts.put("C2", newCourt("Court C2", "Standard", true, clusterC));
        courts.put("C3", newCourt("Court C3", "VIP", true, clusterC));
        courts.put("D1", newCourt("Court D1", "Standard", true, clusterD));
        courts.put("D2", newCourt("Court D2", "Training", true, clusterD));
        courts.put("D3", newCourt("Court D3", "Outdoor", false, clusterD));
        courtRepository.saveAll(courts.values());

        bookingRepository.saveAll(List.of(
            newBooking(users.get("customer.a"), courts.get("A1"), LocalDate.now().plusDays(1), "18:00-19:30", BookingStatus.CONFIRMED, 180000),
            newBooking(users.get("customer.b"), courts.get("A2"), LocalDate.now().plusDays(2), "19:30-21:00", BookingStatus.PENDING, 200000),
            newBooking(users.get("customer.c"), courts.get("B1"), LocalDate.now().minusDays(2), "17:00-18:30", BookingStatus.COMPLETED, 250000),
            newBooking(users.get("customer.d"), courts.get("B2"), LocalDate.now().plusDays(3), "20:00-21:30", BookingStatus.CANCELLED, 220000),
            newBooking(users.get("customer.e"), courts.get("C1"), LocalDate.now().plusDays(1), "16:00-17:30", BookingStatus.PENDING, 170000),
            newBooking(users.get("customer.f"), courts.get("C2"), LocalDate.now().plusDays(4), "19:00-20:30", BookingStatus.CONFIRMED, 210000),
            newBooking(users.get("customer.a"), courts.get("C3"), LocalDate.now().minusDays(4), "18:30-20:00", BookingStatus.COMPLETED, 300000),
            newBooking(users.get("customer.b"), courts.get("D1"), LocalDate.now().plusDays(5), "07:00-08:30", BookingStatus.PENDING, 150000),
            newBooking(users.get("customer.c"), courts.get("D2"), LocalDate.now().plusDays(6), "21:00-22:30", BookingStatus.CONFIRMED, 190000),
            newBooking(users.get("customer.d"), courts.get("D3"), LocalDate.now().minusDays(1), "06:30-08:00", BookingStatus.CANCELLED, 130000),
            newBooking(users.get("customer.e"), courts.get("B3"), LocalDate.now().plusDays(2), "14:00-15:30", BookingStatus.PENDING, 160000),
            newBooking(users.get("customer.f"), courts.get("A3"), LocalDate.now().minusDays(3), "09:00-10:30", BookingStatus.COMPLETED, 175000)
        ));
    }

    private User newUser(String username, String fullName, String email, String phoneNumber, Role role,
            String plainPassword, BCryptPasswordEncoder passwordEncoder) {
        return User.builder().username(username).fullName(fullName).email(email).phoneNumber(phoneNumber)
            .password(passwordEncoder.encode(plainPassword)).role(role).enabled(true).build();
    }

    private Court newCourt(String courtName, String type, boolean available, BadmintonCluster cluster) {
        return Court.builder().courtName(courtName).type(type).isAvailable(available).cluster(cluster).build();
    }

    private Booking newBooking(User user, Court court, LocalDate bookingDate, String timeSlot, BookingStatus status, long price) {
        return Booking.builder().user(user).court(court).bookingDate(bookingDate).timeSlot(timeSlot).status(status)
            .totalPrice(BigDecimal.valueOf(price)).build();
    }
}

