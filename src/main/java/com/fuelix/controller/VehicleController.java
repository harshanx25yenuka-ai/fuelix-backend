package com.fuelix.controller;

import com.fuelix.config.JwtService;
import com.fuelix.dto.QrGenerationResponse;
import com.fuelix.model.FuelLog;
import com.fuelix.model.QrTokenInfo;
import com.fuelix.model.QrVerificationResult;
import com.fuelix.model.Quota;
import com.fuelix.model.Vehicle;
import com.fuelix.model.Wallet;
import com.fuelix.service.FuelLogService;
import com.fuelix.service.FuelPriceService;
import com.fuelix.service.NotificationService;
import com.fuelix.service.QrTokenService;
import com.fuelix.service.QuotaService;
import com.fuelix.service.VehicleService;
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

    @Autowired
    private FuelLogService fuelLogService;

    @Autowired
    private FuelPriceService fuelPriceService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private QrTokenService qrTokenService;

    @Autowired
    private JwtService jwtService;

    // Helper method to extract user ID from token
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }

    // Helper method to extract staff ID from token
    private Long getStaffIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }

    // ==================== BASIC CRUD OPERATIONS ====================

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

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserVehicles(@PathVariable Long userId) {
        List<Vehicle> vehicles = vehicleService.getUserVehicles(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", vehicles);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> addVehicle(@RequestBody Map<String, Object> payload) {
        try {
            Vehicle vehicle = new Vehicle();

            if (payload.containsKey("userId")) {
                vehicle.setUserId(Long.valueOf(payload.get("userId").toString()));
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "userId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if (payload.containsKey("type")) {
                vehicle.setType(payload.get("type").toString());
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "type is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if (payload.containsKey("make")) {
                vehicle.setMake(payload.get("make").toString());
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "make is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if (payload.containsKey("model")) {
                vehicle.setModel(payload.get("model").toString());
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "model is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if (payload.containsKey("year")) {
                vehicle.setYear(payload.get("year").toString());
            } else {
                vehicle.setYear(String.valueOf(LocalDateTime.now().getYear()));
            }

            if (payload.containsKey("registrationNo")) {
                vehicle.setRegistrationNo(payload.get("registrationNo").toString());
            } else {
                vehicle.setRegistrationNo("TEMP-" + System.currentTimeMillis());
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Vehicle vehicleDetails = new Vehicle();

            if (payload.containsKey("type")) {
                vehicleDetails.setType(payload.get("type").toString());
            }
            if (payload.containsKey("make")) {
                vehicleDetails.setMake(payload.get("make").toString());
            }
            if (payload.containsKey("model")) {
                vehicleDetails.setModel(payload.get("model").toString());
            }
            if (payload.containsKey("year")) {
                vehicleDetails.setYear(payload.get("year").toString());
            }
            if (payload.containsKey("registrationNo")) {
                vehicleDetails.setRegistrationNo(payload.get("registrationNo").toString());
            }
            if (payload.containsKey("fuelType")) {
                vehicleDetails.setFuelType(payload.get("fuelType").toString());
            }
            if (payload.containsKey("engineCC")) {
                vehicleDetails.setEngineCC(payload.get("engineCC").toString());
            }
            if (payload.containsKey("color")) {
                vehicleDetails.setColor(payload.get("color").toString());
            }

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

    // ==================== FUEL PASS GENERATION ====================

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

    // ==================== QR CODE VERIFICATION (LEGACY) ====================

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

    // ==================== STAFF QR VERIFICATION (LEGACY) ====================

    @PostMapping("/staff-verify")
    public ResponseEntity<?> staffVerifyPasscode(@RequestBody Map<String, String> payload) {
        try {
            String passcode = payload.get("passcode");
            System.out.println("=== Staff QR Verification (Legacy) ===");
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
            if (balance < 100.0) {
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

    // ==================== DYNAMIC QR TOKEN APIs (NEW - V2) ====================

    /**
     * Generate dynamic QR token for a vehicle
     * Endpoint: POST /api/vehicles/{vehicleId}/dynamic-qr
     *
     * @param vehicleId - ID of the vehicle
     * @param authHeader - Bearer token for authentication
     * @return QR data string with token ID and expiry info
     */
    @PostMapping("/{vehicleId}/dynamic-qr")
    public ResponseEntity<?> generateDynamicQr(
            @PathVariable Long vehicleId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Not authenticated");
                return ResponseEntity.status(401).body(error);
            }

            // Verify vehicle belongs to user
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
            if (!vehicle.getUserId().equals(userId)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Vehicle does not belong to you");
                return ResponseEntity.status(403).body(error);
            }

            // Check if vehicle has fuel pass
            if (vehicle.getFuelPassCode() == null || vehicle.getFuelPassCode().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please generate Fuel Pass first");
                return ResponseEntity.status(400).body(error);
            }

            // Generate token
            QrTokenInfo token = qrTokenService.generateToken(vehicleId, userId);

            // Generate QR payload
            String qrData = qrTokenService.generateQrPayload(token, vehicle);

            QrGenerationResponse response = QrGenerationResponse.builder()
                    .qrData(qrData)
                    .tokenId(token.getTokenId())
                    .expiresIn(300)
                    .generatedAt(System.currentTimeMillis())
                    .message("QR generated successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Staff verify QR code (Version 2 with dynamic token)
     * Endpoint: POST /api/vehicles/staff-verify-v2
     *
     * @param request - Contains qrData string
     * @return Vehicle details if verification succeeds
     */
    @PostMapping("/staff-verify-v2")
    public ResponseEntity<?> staffVerifyQrV2(@RequestBody Map<String, String> request) {
        try {
            String qrData = request.get("qrData");

            if (qrData == null || qrData.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "QR data is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            System.out.println("=== Staff QR Verification V2 ===");
            System.out.println("QR Data length: " + qrData.length());

            // Validate QR
            QrVerificationResult result = qrTokenService.validateQrCode(qrData);

            if (!result.isValid()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", result.getError());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Vehicle vehicle = result.getVehicle();

            // Get quota info
            Quota quota = quotaService.getCurrentQuota(vehicle.getId(), vehicle.getType());

            // Get wallet balance
            Double balance = walletService.getBalance(vehicle.getUserId());

            // Build response with vehicle details
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tokenId", result.getTokenId());
            response.put("message", "QR verified successfully");

            Map<String, Object> vehicleData = new HashMap<>();
            vehicleData.put("id", vehicle.getId());
            vehicleData.put("userId", vehicle.getUserId());
            vehicleData.put("registrationNo", vehicle.getRegistrationNo());
            vehicleData.put("vehicleType", vehicle.getType());
            vehicleData.put("make", vehicle.getMake());
            vehicleData.put("model", vehicle.getModel());
            vehicleData.put("fuelType", vehicle.getFuelType());
            vehicleData.put("year", vehicle.getYear());
            vehicleData.put("color", vehicle.getColor() != null ? vehicle.getColor() : "");
            response.put("vehicle", vehicleData);

            // Add quota info
            Map<String, Object> quotaData = new HashMap<>();
            quotaData.put("remainingQuota", quota.getRemainingLitres());
            quotaData.put("quotaLitres", quota.getQuotaLitres());
            quotaData.put("usedLitres", quota.getUsedLitres());
            response.put("quota", quotaData);

            // Add wallet info
            Map<String, Object> walletData = new HashMap<>();
            walletData.put("balance", balance);
            response.put("wallet", walletData);

            System.out.println("Verification V2 successful for vehicle: " + vehicle.getRegistrationNo());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Verification V2 error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Verification failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Complete fuel refill and mark token as used
     * Endpoint: POST /api/vehicles/complete-refill
     *
     * @param payload - Contains tokenId, staffId, and fuelLogData
     * @return Success response
     */
    @PostMapping("/complete-refill")
    public ResponseEntity<?> completeRefill(@RequestBody Map<String, Object> payload) {
        try {
            String tokenId = (String) payload.get("tokenId");
            Long staffId = ((Number) payload.get("staffId")).longValue();

            if (tokenId == null || tokenId.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Token ID is required");
                return ResponseEntity.badRequest().body(error);
            }

            // Mark token as used
            qrTokenService.markTokenAsUsed(tokenId, staffId);

            // Mark nonce as used to prevent replay
            // Note: You would need to extract nonce from token or pass it in payload

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Refill completed and token invalidated");

            System.out.println("Token marked as used: " + tokenId + " by staff: " + staffId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Invalidate/refresh old token manually
     * Endpoint: DELETE /api/vehicles/qr-token/{tokenId}
     *
     * @param tokenId - Token ID to invalidate
     * @return Success response
     */
    @DeleteMapping("/qr-token/{tokenId}")
    public ResponseEntity<?> invalidateToken(@PathVariable String tokenId) {
        try {
            qrTokenService.invalidateToken(tokenId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token invalidated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get token status (for debugging)
     * Endpoint: GET /api/vehicles/qr-token/{tokenId}/status
     *
     * @param tokenId - Token ID to check
     * @return Token status
     */
    @GetMapping("/qr-token/{tokenId}/status")
    public ResponseEntity<?> getTokenStatus(@PathVariable String tokenId) {
        try {
            String key = "qr:token:" + tokenId;
            Map<String, Object> response = new HashMap<>();
            response.put("tokenId", tokenId);
            response.put("exists", false);
            response.put("used", false);
            response.put("expired", true);

            // This would require additional Redis methods in QrTokenService
            // For now, return basic info
            response.put("message", "Token status check endpoint");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}