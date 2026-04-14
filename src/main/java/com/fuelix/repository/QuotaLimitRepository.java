package com.fuelix.repository;

import com.fuelix.model.QuotaLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuotaLimitRepository extends JpaRepository<QuotaLimit, Long> {
    Optional<QuotaLimit> findByVehicleType(String vehicleType);
    boolean existsByVehicleType(String vehicleType);
}