package com.fuelix.service;

import com.fuelix.model.TopUpTransaction;
import com.fuelix.model.Wallet;
import com.fuelix.repository.TopUpTransactionRepository;
import com.fuelix.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TopUpTransactionRepository transactionRepository;

    public Double getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(0.0);
    }

    @Transactional
    public Wallet topUp(Long userId, Double amount, String method, String reference) {
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElse(new Wallet(userId, 0.0));

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        wallet = walletRepository.save(wallet);

        TopUpTransaction transaction = new TopUpTransaction(
                userId, amount, method, "COMPLETED",
                reference != null ? reference : generateReference()
        );
        transactionRepository.save(transaction);

        return wallet;
    }

    @Transactional
    public void deductBalance(Long userId, Double amount) throws Exception {
        if (amount == null || amount <= 0) {
            throw new Exception("Invalid deduction amount");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new Exception("Wallet not found for user: " + userId));

        if (wallet.getBalance() < amount - 0.001) { // Small epsilon for floating point
            throw new Exception("Insufficient balance. Available: " + wallet.getBalance() + ", Required: " + amount);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
    }

    public List<TopUpTransaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}