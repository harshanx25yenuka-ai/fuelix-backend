package com.fuelix.service;

import com.fuelix.model.FuelLog;
import com.fuelix.repository.FuelLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FuelLogService {

    @Autowired
    private FuelLogRepository fuelLogRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private QuotaService quotaService;

    @Transactional
    public FuelLog saveFuelLog(FuelLog log, String vehicleType) throws Exception {
        // 1. Check wallet balance
        Double balance = walletService.getBalance(log.getUserId());
        if (log.getTotalCost() > balance) {
            throw new Exception("Insufficient wallet balance. Available: " + balance + ", Required: " + log.getTotalCost());
        }

        // 2. Check and deduct quota
        boolean quotaDeducted = quotaService.deductQuota(log.getVehicleId(), vehicleType, log.getLitres());
        if (!quotaDeducted) {
            com.fuelix.model.Quota currentQuota = quotaService.getCurrentQuota(log.getVehicleId(), vehicleType);
            throw new Exception("Weekly quota exceeded. Remaining quota: " + currentQuota.getRemainingLitres() + " L");
        }

        // 3. Deduct from wallet
        walletService.deductBalance(log.getUserId(), log.getTotalCost());

        // 4. Save fuel log
        return fuelLogRepository.save(log);
    }

    public List<FuelLog> getFuelLogsByUser(Long userId) {
        return fuelLogRepository.findByUserIdOrderByLoggedAtDesc(userId);
    }

    public List<FuelLog> getFuelLogsByVehicle(Long vehicleId) {
        return fuelLogRepository.findByVehicleIdOrderByLoggedAtDesc(vehicleId);
    }

    @Transactional
    public void deleteFuelLog(Long id) {
        fuelLogRepository.deleteById(id);
    }

    public Map<String, Double> getFuelLogStats(Long userId) {
        Map<String, Double> stats = new HashMap<>();
        stats.put("totalLogs", fuelLogRepository.getTotalLogs(userId).doubleValue());
        stats.put("totalLitres", fuelLogRepository.getTotalLitres(userId));
        stats.put("totalSpent", fuelLogRepository.getTotalCost(userId));
        return stats;
    }
}