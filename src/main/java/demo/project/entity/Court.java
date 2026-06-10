package demo.project.entity;

import demo.project.entity.BadmintonCluster;
import demo.project.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courts")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "court_name", nullable = false, length = 50)
    private String courtName;

    @Column(length = 50)
    private String type;

    @Column(length = 255)
    private String image;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id")
    private BadmintonCluster cluster;

    @OneToMany(mappedBy = "court", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();
}