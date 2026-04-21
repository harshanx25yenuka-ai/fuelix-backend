package com.fuelix.repository;

import com.fuelix.model.SharedWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SharedWalletRepository extends JpaRepository<SharedWallet, Long> {
    Optional<SharedWallet> findByFamilyId(Long familyId);
}