package com.fuelix.service;

import com.fuelix.model.Quota;
import com.fuelix.model.QuotaLimit;
import com.fuelix.repository.QuotaLimitRepository;
import com.fuelix.repository.QuotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class QuotaService {

    @Autowired
    private QuotaRepository quotaRepository;

    @Autowired
    private QuotaLimitRepository quotaLimitRepository;

    public Double getQuotaForVehicleType(String vehicleType) {
        return quotaLimitRepository.findByVehicleType(vehicleType)
                .map(QuotaLimit::getQuotaLitres)
                .orElse(0.0);
    }

    public List<QuotaLimit> getAllQuotaLimits() {
        return quotaLimitRepository.findAll();
    }

    public QuotaLimit updateQuotaLimit(Long id, Double newQuota, String updatedBy) {
        QuotaLimit limit = quotaLimitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quota limit not found"));
        limit.setQuotaLitres(newQuota);
        limit.setUpdatedAt(LocalDateTime.now());
        limit.setUpdatedBy(updatedBy);
        return quotaLimitRepository.save(limit);
    }

    public LocalDateTime getWeekStart(LocalDateTime date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return date.minusDays(dayOfWeek - 1).with(LocalTime.MIN);
    }

    public LocalDateTime getWeekEnd(LocalDateTime date) {
        return getWeekStart(date).plusDays(6).with(LocalTime.MAX);
    }

    public boolean isCurrentWeek(Quota quota, LocalDateTime now) {
        return !now.isBefore(quota.getWeekStart()) && !now.isAfter(quota.getWeekEnd());
    }

    @Transactional
    public Quota getOrCreateCurrentWeekQuota(Long vehicleId, String vehicleType) {
        LocalDateTime now = LocalDateTime.now();
        Quota existing = quotaRepository.findCurrentWeekQuota(vehicleId, now).orElse(null);

        if (existing != null && isCurrentWeek(existing, now)) {
            // Update quota if limit has changed
            Double currentLimit = getQuotaForVehicleType(vehicleType);
            if (!existing.getQuotaLitres().equals(currentLimit)) {
                existing.setQuotaLitres(currentLimit);
                return quotaRepository.save(existing);
            }
            return existing;
        }

        LocalDateTime weekStart = getWeekStart(now);
        LocalDateTime weekEnd = getWeekEnd(now);
        Double quotaLitres = getQuotaForVehicleType(vehicleType);
        Quota newQuota = new Quota(vehicleId, weekStart, weekEnd, quotaLitres, 0.0);
        return quotaRepository.save(newQuota);
    }

    @Transactional
    public boolean deductQuota(Long vehicleId, String vehicleType, Double litres) {
        Quota quota = getOrCreateCurrentWeekQuota(vehicleId, vehicleType);
        double remaining = quota.getQuotaLitres() - quota.getUsedLitres();
        if (remaining < litres - 0.001) {
            return false;
        }
        quota.setUsedLitres(quota.getUsedLitres() + litres);
        quotaRepository.save(quota);
        return true;
    }

    public List<Quota> getQuotaHistory(Long vehicleId) {
        return quotaRepository.findByVehicleIdOrderByWeekStartDesc(vehicleId);
    }

    public Quota getCurrentQuota(Long vehicleId, String vehicleType) {
        LocalDateTime now = LocalDateTime.now();
        Quota quota = quotaRepository.findCurrentWeekQuota(vehicleId, now)
                .orElse(null);

        if (quota == null) {
            return getOrCreateCurrentWeekQuota(vehicleId, vehicleType);
        }

        // Update quota limit if changed
        Double currentLimit = getQuotaForVehicleType(vehicleType);
        if (!quota.getQuotaLitres().equals(currentLimit)) {
            quota.setQuotaLitres(currentLimit);
            quota = quotaRepository.save(quota);
        }

        return quota;
    }
}