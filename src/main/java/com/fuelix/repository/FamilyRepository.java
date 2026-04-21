package com.fuelix.repository;

import com.fuelix.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    Optional<Family> findByCreatedBy(Long createdBy);
    boolean existsByCreatedBy(Long createdBy);
}