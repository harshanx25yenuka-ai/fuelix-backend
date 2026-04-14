package com.fuelix.controller;

import com.fuelix.model.FuelPrice;
import com.fuelix.service.FuelPriceService;
import com.fuelix.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/fuel-prices")
public class FuelPriceController {

    @Autowired
    private FuelPriceService fuelPriceService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getAllFuelPrices() {
        List<FuelPrice> prices = fuelPriceService.getAllFuelPrices();
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFuelPriceById(@PathVariable Long id) {
        try {
            FuelPrice price = fuelPriceService.getFuelPriceById(id);
            return ResponseEntity.ok(price);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/grade/{fuelGrade}")
    public ResponseEntity<?> getFuelPriceByGrade(@PathVariable String fuelGrade) {
        try {
            FuelPrice price = fuelPriceService.getFuelPriceByGrade(fuelGrade);
            return ResponseEntity.ok(price);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFuelPrice(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Double newPrice = Double.valueOf(payload.get("pricePerLitre").toString());
            String updatedBy = payload.get("updatedBy") != null ? payload.get("updatedBy").toString() : "admin";

            FuelPrice oldPrice = fuelPriceService.getFuelPriceById(id);
            FuelPrice updated = fuelPriceService.updateFuelPrice(id, newPrice, updatedBy);

            // Create single notification for single update
            List<NotificationService.FuelPriceChange> changes = new ArrayList<>();
            changes.add(new NotificationService.FuelPriceChange(updated.getFuelGrade(), oldPrice.getPricePerLitre(), newPrice));
            notificationService.notifyBulkFuelPriceUpdate(changes, updatedBy);

            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/bulk-update")
    public ResponseEntity<?> bulkUpdateFuelPrices(@RequestBody List<FuelPriceService.FuelPriceUpdateRequest> updates) {
        try {
            String updatedBy = "admin";

            // Get old prices before update and collect changes
            List<NotificationService.FuelPriceChange> changes = new ArrayList<>();
            for (FuelPriceService.FuelPriceUpdateRequest update : updates) {
                FuelPrice oldPrice = fuelPriceService.getFuelPriceById(update.getId());
                if (!oldPrice.getPricePerLitre().equals(update.getPricePerLitre())) {
                    changes.add(new NotificationService.FuelPriceChange(
                            oldPrice.getFuelGrade(),
                            oldPrice.getPricePerLitre(),
                            update.getPricePerLitre()
                    ));
                }
            }

            // Perform the bulk update
            List<FuelPrice> updatedPrices = fuelPriceService.bulkUpdateFuelPrices(updates, updatedBy);

            // Create SINGLE notification for ALL changes (NOT multiple notifications)
            if (!changes.isEmpty()) {
                notificationService.notifyBulkFuelPriceUpdate(changes, updatedBy);
                System.out.println("✅ Created 1 notification for " + changes.size() + " fuel price changes");
            } else {
                System.out.println("⚠️ No fuel price changes detected");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fuel prices updated successfully");
            response.put("data", updatedPrices);
            response.put("changes", changes);
            response.put("notificationCount", changes.isEmpty() ? 0 : 1);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/initialize")
    public ResponseEntity<?> initializeDefaultFuelPrices() {
        fuelPriceService.initializeDefaultFuelPrices();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Default fuel prices initialized");
        return ResponseEntity.ok(response);
    }
}