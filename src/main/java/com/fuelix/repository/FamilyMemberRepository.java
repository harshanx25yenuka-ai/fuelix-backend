package com.fuelix.repository;

import com.fuelix.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);
    List<FamilyMember> findByFamilyIdAndIsActiveTrue(Long familyId);
    List<FamilyMember> findByUserId(Long userId);
    List<FamilyMember> findByUserIdAndIsActiveFalse(Long userId);
    Optional<FamilyMember> findByFamilyIdAndUserIdAndIsActiveFalse(Long familyId, Long userId);
    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);
    boolean existsByFamilyIdAndUserIdAndIsActiveFalse(Long familyId, Long userId);
    long countByFamilyIdAndIsActiveTrue(Long familyId);
}