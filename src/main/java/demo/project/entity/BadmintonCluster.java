package demo.project.entity;

import demo.project.entity.Court;
import demo.project.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "badminton_clusters")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
public class BadmintonCluster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(name = "hot_line", length = 20)
    private String hotLine;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", unique = true)
    private User manager;

    @OneToMany(mappedBy = "cluster", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Court> courts = new ArrayList<>();
}