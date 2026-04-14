package com.fuelix.repository;

import com.fuelix.model.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {
    Optional<VehicleBrand> findByBrandName(String brandName);
    boolean existsByBrandName(String brandName);

    @Query("SELECT DISTINCT v.brandName FROM VehicleBrand v ORDER BY v.brandName")
    List<String> findAllBrandNames();
}