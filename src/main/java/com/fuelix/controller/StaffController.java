package com.fuelix.controller;

import com.fuelix.config.JwtService;
import com.fuelix.dto.LoginRequest;
import com.fuelix.dto.LoginResponse;
import com.fuelix.dto.StaffAuthResponse;
import com.fuelix.dto.StaffRequest;
import com.fuelix.dto.StaffResponse;
import com.fuelix.model.FuelStation;
import com.fuelix.model.Staff;
import com.fuelix.model.User;
import com.fuelix.service.AuthService;
import com.fuelix.service.StaffService;
import com.fuelix.service.FuelStationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @Autowired
    private AuthService authService;

    @Autowired
    private FuelStationService fuelStationService;

    @Autowired
    private JwtService jwtService;

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }

    // Staff authentication endpoint for QR scanner
    @PostMapping("/auth")
    public ResponseEntity<?> authenticateStaff(@Valid @RequestBody LoginRequest request) {
        try {
            System.out.println("=== Staff Authentication Request ===");
            System.out.println("NIC: " + request.getNic());

            // Step 1: Authenticate user with NIC and password
            LoginResponse loginResponse = authService.login(request);

            System.out.println("Login Response - Role: " + loginResponse.getRole());
            System.out.println("Login Response - User ID: " + loginResponse.getId());
            System.out.println("Login Response - FirstName: " + loginResponse.getFirstName());

            if (loginResponse == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid NIC or password"));
            }

            // Step 2: Check if role is STAFF
            String userRole = loginResponse.getRole();
            System.out.println("User Role from LoginResponse: " + userRole);

            if (userRole == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User role not found. Please contact support."));
            }

            if (!"STAFF".equalsIgnoreCase(userRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied. Only staff members can use the QR scanner. Your role: " + userRole));
            }

            // Step 3: Verify staff record exists
            Long userId = loginResponse.getId();
            Staff staffRecord = staffService.getStaffByUserId(userId);

            System.out.println("Staff Record: " + (staffRecord != null ? staffRecord.getId() : "null"));

            if (staffRecord == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Staff record not found. Please contact your station owner."));
            }

            // Step 4: Get station details
            Long stationId = staffRecord.getStationId();
            FuelStation station = fuelStationService.getStationById(stationId);

            System.out.println("Station: " + (station != null ? station.getName() : "null"));

            if (station == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Fuel station not found for this staff member."));
            }

            // Step 5: Build response with station info
            StaffAuthResponse response = new StaffAuthResponse();
            response.setSuccess(true);
            response.setToken(loginResponse.getToken());
            response.setUserId(userId);
            response.setStaffId(staffRecord.getStaffId());
            response.setStationId(stationId);
            response.setStationName(station.getName());
            response.setStationBrand(station.getBrand() != null ? station.getBrand() : "");
            response.setStaffName(loginResponse.getFirstName() + " " + loginResponse.getLastName());
            response.setRole(loginResponse.getRole());
            response.setFirstName(loginResponse.getFirstName());
            response.setLastName(loginResponse.getLastName());
            response.setNic(loginResponse.getNic());
            response.setEmail(loginResponse.getEmail());
            response.setMobile(loginResponse.getMobile());
            response.setMessage("Authentication successful");

            System.out.println("Authentication successful for: " + loginResponse.getFirstName());
            System.out.println("Response Role: " + response.getRole());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("RuntimeException: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addStaff(@Valid @RequestBody StaffRequest request,
                                      @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            StaffResponse response = staffService.addStaff(request, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Staff member added successfully");
            result.put("staff", response);
            result.put("success", true);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to add staff: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeStaff(@RequestParam Long staffId,
                                         @RequestParam Long stationId,
                                         @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            staffService.removeStaff(staffId, stationId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Staff member removed successfully");
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to remove staff: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<?> getStaffByStation(@PathVariable Long stationId,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            List<StaffResponse> staffList = staffService.getStaffByStation(stationId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("staff", staffList);
            response.put("count", staffList.size());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get staff list: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/count/{stationId}")
    public ResponseEntity<?> getStaffCount(@PathVariable Long stationId,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            long count = staffService.getStaffCount(stationId, userId);

            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }
}