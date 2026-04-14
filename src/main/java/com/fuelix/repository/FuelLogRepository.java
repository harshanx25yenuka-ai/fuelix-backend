package com.fuelix.repository;

import com.fuelix.model.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {
    List<FuelLog> findByUserIdOrderByLoggedAtDesc(Long userId);
    List<FuelLog> findByVehicleIdOrderByLoggedAtDesc(Long vehicleId);

    @Query("SELECT COALESCE(SUM(f.litres), 0) FROM FuelLog f WHERE f.userId = :userId")
    Double getTotalLitres(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(f.totalCost), 0) FROM FuelLog f WHERE f.userId = :userId")
    Double getTotalCost(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FuelLog f WHERE f.userId = :userId")
    Long getTotalLogs(@Param("userId") Long userId);

    // Delete all fuel logs for a vehicle
    void deleteByVehicleId(Long vehicleId);
}