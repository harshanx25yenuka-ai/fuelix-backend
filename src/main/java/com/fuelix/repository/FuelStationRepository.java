package com.fuelix.repository;

import com.fuelix.model.FuelStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FuelStationRepository extends JpaRepository<FuelStation, Long> {
    List<FuelStation> findByProvince(String province);
    List<FuelStation> findByDistrict(String district);
    List<FuelStation> findByBrand(String brand);
    List<FuelStation> findByIsFuelixPartnerTrue();

    Optional<FuelStation> findByOwnerId(Long ownerId);

    @Query("SELECT f FROM FuelStation f WHERE f.isOpen = true")
    List<FuelStation> findAllOpen();

    @Query("SELECT f FROM FuelStation f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<FuelStation> searchByName(@Param("query") String query);

    boolean existsByOwnerId(Long ownerId);
}