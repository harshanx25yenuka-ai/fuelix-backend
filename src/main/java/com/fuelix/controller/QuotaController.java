package com.fuelix.controller;

import com.fuelix.model.Quota;
import com.fuelix.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotas")
public class QuotaController {

    @Autowired
    private QuotaService quotaService;

    @GetMapping("/current/{vehicleId}")
    public ResponseEntity<?> getCurrentQuota(@PathVariable Long vehicleId, @RequestParam String vehicleType) {
        try {
            Quota quota = quotaService.getCurrentQuota(vehicleId, vehicleType);
            Map<String, Object> response = new HashMap<>();
            response.put("id", quota.getId());
            response.put("vehicleId", quota.getVehicleId());
            response.put("weekStart", quota.getWeekStart());
            response.put("weekEnd", quota.getWeekEnd());
            response.put("quotaLitres", quota.getQuotaLitres());
            response.put("usedLitres", quota.getUsedLitres());
            response.put("remainingLitres", quota.getRemainingLitres());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/history/{vehicleId}")
    public ResponseEntity<?> getQuotaHistory(@PathVariable Long vehicleId) {
        List<Quota> history = quotaService.getQuotaHistory(vehicleId);
        return ResponseEntity.ok(history);
    }
}