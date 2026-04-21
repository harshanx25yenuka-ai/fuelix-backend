package com.fuelix.repository;

import com.fuelix.model.SharedWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SharedWalletTransactionRepository extends JpaRepository<SharedWalletTransaction, Long> {
    List<SharedWalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<SharedWalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}