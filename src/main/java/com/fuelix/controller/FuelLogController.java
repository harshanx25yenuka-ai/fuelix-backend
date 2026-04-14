package com.fuelix.controller;

import com.fuelix.model.FuelLog;
import com.fuelix.model.Vehicle;
import com.fuelix.service.FuelLogService;
import com.fuelix.service.FuelPriceService;
import com.fuelix.service.NotificationService;
import com.fuelix.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fuel-logs")
public class FuelLogController {

    @Autowired
    private FuelLogService fuelLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private FuelPriceService fuelPriceService;

    @PostMapping
    public ResponseEntity<?> addFuelLog(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("Received payload: " + payload);

            String[] requiredFields = {"userId", "vehicleId", "litres", "fuelType", "fuelGrade", "vehicleType"};
            for (String field : requiredFields) {
                if (!payload.containsKey(field) || payload.get(field) == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", field + " is required");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }

            Long userId;
            Long vehicleId;
            Double litres;

            try {
                userId = ((Number) payload.get("userId")).longValue();
                vehicleId = ((Number) payload.get("vehicleId")).longValue();
                litres = ((Number) payload.get("litres")).doubleValue();
            } catch (ClassCastException e) {
                try {
                    userId = Long.parseLong(payload.get("userId").toString());
                    vehicleId = Long.parseLong(payload.get("vehicleId").toString());
                    litres = Double.parseDouble(payload.get("litres").toString());
                } catch (NumberFormatException ex) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid number format: " + ex.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }

            Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
            if (vehicle.getFuelPassCode() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please generate Fuel Pass for this vehicle before logging fuel.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            String fuelType = payload.get("fuelType").toString();
            String fuelGrade = payload.get("fuelGrade").toString();
            String stationName = payload.get("stationName") != null ? payload.get("stationName").toString() : "";
            String vehicleType = payload.get("vehicleType").toString();

            var fuelPrice = fuelPriceService.getFuelPriceByGrade(fuelGrade);
            Double pricePerLitre = fuelPrice.getPricePerLitre();
            Double totalCost = litres * pricePerLitre;

            FuelLog log = new FuelLog();
            log.setUserId(userId);
            log.setVehicleId(vehicleId);
            log.setLitres(litres);
            log.setFuelType(fuelType);
            log.setFuelGrade(fuelGrade);
            log.setPricePerLitre(pricePerLitre);
            log.setTotalCost(totalCost);
            log.setStationName(stationName);
            log.setLoggedAt(LocalDateTime.now());

            FuelLog saved = fuelLogService.saveFuelLog(log, vehicleType);

            String vehicleName = vehicle.getMake() + " " + vehicle.getModel();
            notificationService.notifyFuelLogCreated(userId, vehicleName, litres, fuelGrade, totalCost);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", saved);
            response.put("message", "Fuel log saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserFuelLogs(@PathVariable Long userId) {
        List<FuelLog> logs = fuelLogService.getFuelLogsByUser(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<?> getVehicleFuelLogs(@PathVariable Long vehicleId) {
        List<FuelLog> logs = fuelLogService.getFuelLogsByVehicle(vehicleId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getStats(@PathVariable Long userId) {
        Map<String, Double> stats = fuelLogService.getFuelLogStats(userId);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFuelLog(@PathVariable Long id) {
        fuelLogService.deleteFuelLog(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Fuel log deleted");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prices")
    public ResponseEntity<?> getAllFuelPrices() {
        List<com.fuelix.model.FuelPrice> prices = fuelPriceService.getAllFuelPrices();
        return ResponseEntity.ok(prices);
    }
}