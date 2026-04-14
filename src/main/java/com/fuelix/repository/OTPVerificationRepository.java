package com.fuelix.repository;

import com.fuelix.model.OTPVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {

    Optional<OTPVerification> findTopByIdentifierAndTypeOrderByCreatedAtDesc(String identifier, String type);

    @Modifying
    @Transactional
    @Query("DELETE FROM OTPVerification o WHERE o.identifier = :identifier AND o.type = :type")
    void deleteByIdentifierAndType(@Param("identifier") String identifier, @Param("type") String type);
}