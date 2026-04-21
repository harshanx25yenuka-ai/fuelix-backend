package com.fuelix.repository;

import com.fuelix.model.SharedVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SharedVehicleRepository extends JpaRepository<SharedVehicle, Long> {
    List<SharedVehicle> findBySharedWithUserIdAndIsActiveTrue(Long sharedWithUserId);
    List<SharedVehicle> findByOwnerUserIdAndIsActiveTrue(Long ownerUserId);
    Optional<SharedVehicle> findByVehicleIdAndSharedWithUserId(Long vehicleId, Long sharedWithUserId);
    boolean existsByVehicleIdAndSharedWithUserId(Long vehicleId, Long sharedWithUserId);
    void deleteByVehicleIdAndSharedWithUserId(Long vehicleId, Long sharedWithUserId);
}