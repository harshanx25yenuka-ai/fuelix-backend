package com.fuelix.repository;

import com.fuelix.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByUserId(Long userId);
    Optional<Vehicle> findByRegistrationNo(String registrationNo);
    boolean existsByRegistrationNoAndUserId(String registrationNo, Long userId);
    Optional<Vehicle> findByFuelPassCode(String fuelPassCode);
    boolean existsByFuelPassCode(String fuelPassCode);
}