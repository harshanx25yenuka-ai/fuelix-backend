package com.fuelix.repository;

import com.fuelix.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    List<VehicleModel> findByBrandIdOrderByModelNameAsc(Long brandId);
    List<VehicleModel> findByVehicleTypeOrderByModelNameAsc(String vehicleType);
    List<VehicleModel> findByBrandIdAndVehicleTypeOrderByModelNameAsc(Long brandId, String vehicleType);

    @Query("SELECT DISTINCT m.modelName FROM VehicleModel m WHERE m.brandId = :brandId AND m.vehicleType = :vehicleType")
    List<String> findModelNamesByBrandIdAndVehicleType(@Param("brandId") Long brandId, @Param("vehicleType") String vehicleType);

    boolean existsByBrandIdAndModelNameAndVehicleType(Long brandId, String modelName, String vehicleType);
}