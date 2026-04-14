package com.fuelix.controller;

import com.fuelix.model.QuotaLimit;
import com.fuelix.repository.QuotaLimitRepository;
import com.fuelix.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/quotas")
public class QuotaLimitController {

    @Autowired
    private QuotaLimitRepository quotaLimitRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/limits")
    public ResponseEntity<?> getAllQuotaLimits() {
        List<QuotaLimit> limits = quotaLimitRepository.findAll();
        return ResponseEntity.ok(limits);
    }

    @GetMapping("/limits/{vehicleType}")
    public ResponseEntity<?> getQuotaLimitByVehicleType(@PathVariable String vehicleType) {
        return quotaLimitRepository.findByVehicleType(vehicleType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/limits/bulk-update")
    public ResponseEntity<?> bulkUpdateQuotaLimits(@RequestBody List<Map<String, Object>> updates) {
        try {
            List<QuotaLimit> updatedLimits = new ArrayList<>();
            List<NotificationService.QuotaChange> changes = new ArrayList<>();

            for (Map<String, Object> update : updates) {
                Long id = Long.valueOf(update.get("id").toString());
                Double newQuota = Double.valueOf(update.get("weeklyQuota").toString());
                String updatedBy = update.get("updatedBy") != null ? update.get("updatedBy").toString() : "admin";

                QuotaLimit limit = quotaLimitRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Quota limit not found for id: " + id));

                Double oldQuota = limit.getQuotaLitres();

                if (!oldQuota.equals(newQuota)) {
                    changes.add(new NotificationService.QuotaChange(limit.getVehicleType(), oldQuota, newQuota));
                    limit.setQuotaLitres(newQuota);
                    limit.setUpdatedAt(LocalDateTime.now());
                    limit.setUpdatedBy(updatedBy);
                    QuotaLimit saved = quotaLimitRepository.save(limit);
                    updatedLimits.add(saved);
                } else {
                    updatedLimits.add(limit);
                }
            }

            if (!changes.isEmpty()) {
                notificationService.notifyBulkQuotaUpdate(changes, "admin");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Quota limits updated successfully (effective from next Monday)");
            response.put("data", updatedLimits);
            response.put("changes", changes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/limits/{id}")
    public ResponseEntity<?> updateQuotaLimit(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            QuotaLimit limit = quotaLimitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Quota limit not found"));

            Double oldQuota = limit.getQuotaLitres();
            Double newQuota = Double.valueOf(payload.get("quotaLitres").toString());
            String updatedBy = payload.get("updatedBy") != null ? payload.get("updatedBy").toString() : "admin";

            limit.setQuotaLitres(newQuota);
            limit.setUpdatedAt(LocalDateTime.now());
            limit.setUpdatedBy(updatedBy);

            QuotaLimit saved = quotaLimitRepository.save(limit);

            List<NotificationService.QuotaChange> changes = new ArrayList<>();
            changes.add(new NotificationService.QuotaChange(limit.getVehicleType(), oldQuota, newQuota));
            notificationService.notifyBulkQuotaUpdate(changes, updatedBy);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/limits/initialize")
    public ResponseEntity<?> initializeDefaultQuotaLimits() {
        String[] vehicleTypes = {"Car", "Van", "Motorcycle", "Truck", "Bus", "Three-Wheeler"};
        Double[] defaultQuotas = {25.0, 25.0, 2.0, 20.0, 45.0, 15.0};

        List<NotificationService.QuotaChange> changes = new ArrayList<>();

        for (int i = 0; i < vehicleTypes.length; i++) {
            if (!quotaLimitRepository.existsByVehicleType(vehicleTypes[i])) {
                QuotaLimit limit = new QuotaLimit(vehicleTypes[i], defaultQuotas[i]);
                limit.setUpdatedBy("system");
                quotaLimitRepository.save(limit);
                changes.add(new NotificationService.QuotaChange(vehicleTypes[i], 0.0, defaultQuotas[i]));
            }
        }

        if (!changes.isEmpty()) {
            notificationService.notifyBulkQuotaUpdate(changes, "system");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Default quota limits initialized");
        return ResponseEntity.ok(response);
    }
}