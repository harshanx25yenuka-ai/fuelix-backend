package com.fuelix.controller;

import com.fuelix.model.Vehicle;
import com.fuelix.model.Quota;
import com.fuelix.model.Wallet;
import com.fuelix.service.VehicleService;
import com.fuelix.service.QuotaService;
import com.fuelix.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private WalletService walletService;

    @GetMapping
    public ResponseEntity<?> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", vehicles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable Long id) {
        try {
            Vehicle vehicle = vehicleService.getVehicleById(id);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/verify-passcode")
    public ResponseEntity<?> verifyVehiclePasscode(@RequestParam String passcode) {
        try {
            Vehicle vehicle = vehicleService.getVehicleByPassCode(passcode);

            Map<String, Object> response = new HashMap<>();
            response.put("id", vehicle.getId());
            response.put("userId", vehicle.getUserId());
            response.put("type", vehicle.getType());
            response.put("make", vehicle.getMake());
            response.put("model", vehicle.getModel());
            response.put("year", vehicle.getYear());
            response.put("registrationNo", vehicle.getRegistrationNo());
            response.put("fuelType", vehicle.getFuelType());
            response.put("engineCC", vehicle.getEngineCC() != null ? vehicle.getEngineCC() : "");
            response.put("color", vehicle.getColor() != null ? vehicle.getColor() : "");
            response.put("qrGeneratedAt", vehicle.getQrGeneratedAt());
            response.put("createdAt", vehicle.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // Staff QR Verification - Full verification with quota and wallet
    @PostMapping("/staff-verify")
    public ResponseEntity<?> staffVerifyPasscode(@RequestBody Map<String, String> payload) {
        try {
            String passcode = payload.get("passcode");
            System.out.println("=== Staff QR Verification ===");
            System.out.println("Passcode: " + passcode);

            // Step 1: Verify fuel pass code
            Vehicle vehicle = vehicleService.getVehicleByPassCode(passcode);

            Map<String, Object> response = new HashMap<>();

            // Step 1 response
            Map<String, Object> step1 = new HashMap<>();
            step1.put("success", true);
            step1.put("message", "Fuel pass code verified");
            step1.put("vehicleId", vehicle.getId());
            step1.put("userId", vehicle.getUserId());
            step1.put("registrationNo", vehicle.getRegistrationNo());
            step1.put("vehicleType", vehicle.getType());
            step1.put("make", vehicle.getMake());
            step1.put("model", vehicle.getModel());
            response.put("step1", step1);

            // Step 2: Check available quota
            Quota quota = quotaService.getCurrentQuota(vehicle.getId(), vehicle.getType());
            double remainingQuota = quota.getRemainingLitres();

            Map<String, Object> step2 = new HashMap<>();
            if (remainingQuota < 1.0) {
                step2.put("success", false);
                step2.put("message", "Insufficient quota available. Remaining: " + String.format("%.1f", remainingQuota) + " L");
                step2.put("remainingQuota", remainingQuota);
                step2.put("quotaLitres", quota.getQuotaLitres());
                step2.put("usedLitres", quota.getUsedLitres());
                response.put("step2", step2);
                response.put("success", false);
                response.put("error", "Quota insufficient");
                return ResponseEntity.ok(response);
            }

            step2.put("success", true);
            step2.put("message", "Quota available");
            step2.put("remainingQuota", remainingQuota);
            step2.put("quotaLitres", quota.getQuotaLitres());
            step2.put("usedLitres", quota.getUsedLitres());
            response.put("step2", step2);

            // Step 3: Check wallet balance
            Double balance = walletService.getBalance(vehicle.getUserId());

            Map<String, Object> step3 = new HashMap<>();
            if (balance < 100.0) { // Minimum balance check
                step3.put("success", false);
                step3.put("message", "Low wallet balance. Current balance: LKR " + String.format("%.2f", balance));
                step3.put("balance", balance);
                response.put("step3", step3);
                response.put("success", false);
                response.put("error", "Insufficient balance");
                return ResponseEntity.ok(response);
            }

            step3.put("success", true);
            step3.put("message", "Wallet balance sufficient");
            step3.put("balance", balance);
            response.put("step3", step3);

            // Step 4: Load all fuel pass data
            Map<String, Object> step4 = new HashMap<>();
            step4.put("success", true);
            step4.put("message", "Data loaded successfully");
            step4.put("userId", vehicle.getUserId());
            step4.put("registrationNo", vehicle.getRegistrationNo());
            step4.put("remainingQuota", remainingQuota);
            step4.put("balance", balance);
            step4.put("vehicleId", vehicle.getId());
            step4.put("vehicleType", vehicle.getType());
            step4.put("make", vehicle.getMake());
            step4.put("model", vehicle.getModel());
            step4.put("fuelType", vehicle.getFuelType());
            response.put("step4", step4);

            response.put("success", true);
            response.put("message", "Verification complete");

            System.out.println("Verification successful for vehicle: " + vehicle.getRegistrationNo());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            Map<String, Object> step1 = new HashMap<>();
            step1.put("success", false);
            step1.put("message", e.getMessage());
            error.put("step1", step1);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Verification failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/by-passcode/{passcode}")
    public ResponseEntity<?> getVehicleByPasscode(@PathVariable String passcode) {
        try {
            Vehicle vehicle = vehicleService.getVehicleByPassCode(passcode);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> addVehicle(@RequestBody Map<String, Object> payload) {
        try {
            Vehicle vehicle = new Vehicle();

            if (payload.containsKey("userId")) {
                vehicle.setUserId(Long.valueOf(payload.get("userId").toString()));
            } else {
                vehicle.setUserId(1L);
            }

            vehicle.setType(payload.get("type").toString());
            vehicle.setMake(payload.get("make").toString());
            vehicle.setModel(payload.get("model").toString());

            if (payload.containsKey("year")) {
                vehicle.setYear(payload.get("year").toString());
            } else {
                vehicle.setYear(String.valueOf(LocalDateTime.now().getYear()));
            }

            if (payload.containsKey("registrationNo")) {
                vehicle.setRegistrationNo(payload.get("registrationNo").toString());
            } else {
                vehicle.setRegistrationNo("VEH-" + System.currentTimeMillis());
            }

            if (payload.containsKey("fuelType")) {
                vehicle.setFuelType(payload.get("fuelType").toString());
            } else {
                vehicle.setFuelType("Petrol");
            }

            vehicle.setEngineCC(payload.getOrDefault("engineCC", "").toString());
            vehicle.setColor(payload.getOrDefault("color", "").toString());
            vehicle.setCreatedAt(LocalDateTime.now());

            Vehicle saved = vehicleService.addVehicle(vehicle);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vehicle added successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid request data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserVehicles(@PathVariable Long userId) {
        List<Vehicle> vehicles = vehicleService.getUserVehicles(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", vehicles);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Vehicle vehicleDetails = new Vehicle();
            vehicleDetails.setType(payload.get("type").toString());
            vehicleDetails.setMake(payload.get("make").toString());
            vehicleDetails.setModel(payload.get("model").toString());
            vehicleDetails.setYear(payload.get("year").toString());
            vehicleDetails.setRegistrationNo(payload.get("registrationNo").toString());
            vehicleDetails.setFuelType(payload.get("fuelType").toString());
            vehicleDetails.setEngineCC(payload.getOrDefault("engineCC", "").toString());
            vehicleDetails.setColor(payload.getOrDefault("color", "").toString());

            Vehicle updated = vehicleService.updateVehicle(id, vehicleDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vehicle updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(id);
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Vehicle deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/{id}/generate-pass")
    public ResponseEntity<?> generateFuelPass(@PathVariable Long id) {
        try {
            Vehicle vehicle = vehicleService.generateFuelPass(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fuel Pass generated successfully");
            response.put("data", vehicle);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/regenerate-pass/{id}")
    public ResponseEntity<?> regenerateFuelPass(@PathVariable Long id) {
        try {
            Vehicle vehicle = vehicleService.regenerateFuelPass(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fuel Pass regenerated successfully");
            response.put("data", vehicle);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/check-passcode/{passcode}")
    public ResponseEntity<?> checkPasscodeExists(@PathVariable String passcode) {
        try {
            boolean exists = vehicleService.isPasscodeValid(passcode);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}