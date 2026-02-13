package com.iot.sensors.repository;

import com.iot.sensors.model.GymUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GymUserRepository extends JpaRepository<GymUser, Long> {
    Optional<GymUser> findByRfidTag(String rfidTag);
    boolean existsByRfidTag(String rfidTag);
    long countByInsideTrue();
}
