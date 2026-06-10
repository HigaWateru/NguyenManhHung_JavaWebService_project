package demo.project.repository;

import demo.project.entity.BadmintonCluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadmintonClusterRepository extends JpaRepository<BadmintonCluster, Long> {
}

