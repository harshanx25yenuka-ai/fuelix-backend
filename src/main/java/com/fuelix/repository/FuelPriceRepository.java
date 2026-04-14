package com.fuelix.repository;

import com.fuelix.model.FuelPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long> {
    Optional<FuelPrice> findByFuelGrade(String fuelGrade);
    List<FuelPrice> findAllByOrderByFuelTypeAscFuelGradeAsc();
    boolean existsByFuelGrade(String fuelGrade);
}