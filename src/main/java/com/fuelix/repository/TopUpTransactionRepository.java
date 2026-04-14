package com.fuelix.repository;

import com.fuelix.model.TopUpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopUpTransactionRepository extends JpaRepository<TopUpTransaction, Long> {
    List<TopUpTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}