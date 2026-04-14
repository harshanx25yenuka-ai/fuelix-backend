package com.fuelix.repository;

import com.fuelix.model.Quota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuotaRepository extends JpaRepository<Quota, Long> {
    Optional<Quota> findTopByVehicleIdOrderByWeekStartDesc(Long vehicleId);
    List<Quota> findByVehicleIdOrderByWeekStartDesc(Long vehicleId);

    @Query("SELECT q FROM Quota q WHERE q.vehicleId = :vehicleId AND q.weekStart <= :now AND q.weekEnd >= :now")
    Optional<Quota> findCurrentWeekQuota(@Param("vehicleId") Long vehicleId, @Param("now") LocalDateTime now);

    void deleteByVehicleId(Long vehicleId);
}