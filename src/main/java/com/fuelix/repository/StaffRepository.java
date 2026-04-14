package com.fuelix.repository;

import com.fuelix.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByStationId(Long stationId);

    Optional<Staff> findByStaffId(Long staffId);  // Add this method

    Optional<Staff> findByStaffIdAndStationId(Long staffId, Long stationId);

    boolean existsByStaffIdAndStationId(Long staffId, Long stationId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Staff s WHERE s.staffId = :staffId AND s.stationId = :stationId")
    void deleteByStaffIdAndStationId(@Param("staffId") Long staffId, @Param("stationId") Long stationId);

    @Query("SELECT s.staffId FROM Staff s WHERE s.stationId = :stationId")
    List<Long> findStaffIdsByStationId(@Param("stationId") Long stationId);

    long countByStationId(Long stationId);
}