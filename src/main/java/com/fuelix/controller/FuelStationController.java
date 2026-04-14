package com.fuelix.controller;

import com.fuelix.config.JwtService;
import com.fuelix.model.FuelStation;
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
@RequestMapping("/api/fuel-stations")
public class FuelStationController {

    @Autowired
    private FuelStationService fuelStationService;

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

    @GetMapping
    public ResponseEntity<?> getAllStations() {
        List<FuelStation> stations = fuelStationService.getAllStations();
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStationById(@PathVariable Long id) {
        try {
            FuelStation station = fuelStationService.getStationById(id);
            return ResponseEntity.ok(station);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    // Get station by owner (logged in user) - FIXED 403 ERROR
    @GetMapping("/user/my-station")
    public ResponseEntity<?> getMyStation(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(401).body(error);
            }

            FuelStation station = fuelStationService.getStationByOwnerId(userId);
            if (station == null) {
                return ResponseEntity.status(404).body(Map.of("error", "No station found for this user"));
            }
            return ResponseEntity.ok(station);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Check if user has a station
    @GetMapping("/user/has-station")
    public ResponseEntity<?> userHasStation(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("hasStation", false));
            }

            boolean hasStation = fuelStationService.userHasStation(userId);
            return ResponseEntity.ok(Map.of("hasStation", hasStation));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasStation", false));
        }
    }

    @GetMapping("/province/{province}")
    public ResponseEntity<?> getStationsByProvince(@PathVariable String province) {
        List<FuelStation> stations = fuelStationService.getStationsByProvince(province);
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/district/{district}")
    public ResponseEntity<?> getStationsByDistrict(@PathVariable String district) {
        List<FuelStation> stations = fuelStationService.getStationsByDistrict(district);
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<?> getStationsByBrand(@PathVariable String brand) {
        List<FuelStation> stations = fuelStationService.getStationsByBrand(brand);
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/partners")
    public ResponseEntity<?> getPartnerStations() {
        List<FuelStation> stations = fuelStationService.getPartnerStations();
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/open")
    public ResponseEntity<?> getOpenStations() {
        List<FuelStation> stations = fuelStationService.getOpenStations();
        return ResponseEntity.ok(stations);
    }

    // CREATE - Add new fuel station
    @PostMapping
    public ResponseEntity<?> createStation(@Valid @RequestBody FuelStation station,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(401).body(error);
            }

            // Check if user already has a station
            if (fuelStationService.userHasStation(userId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You already own a fuel station. Each user can only create one station."));
            }

            // Validate required fields
            Map<String, String> validationErrors = validateStation(station);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest().body(validationErrors);
            }

            station.setOwnerId(userId);
            FuelStation savedStation = fuelStationService.addStation(station);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Fuel station created successfully");
            response.put("station", savedStation);
            response.put("success", true);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create fuel station: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // UPDATE - Update existing fuel station
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStation(@PathVariable Long id,
                                           @Valid @RequestBody FuelStation station,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            FuelStation updatedStation = fuelStationService.updateStation(id, station, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Fuel station updated successfully");
            response.put("station", updatedStation);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update fuel station: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // DELETE - Delete fuel station
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStation(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            fuelStationService.deleteStation(id, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Fuel station deleted successfully");
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete fuel station: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Search stations by name
    @GetMapping("/search")
    public ResponseEntity<?> searchStations(@RequestParam String query) {
        try {
            List<FuelStation> stations = fuelStationService.searchStationsByName(query);
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Get stations with filters
    @GetMapping("/filter")
    public ResponseEntity<?> filterStations(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isOpen,
            @RequestParam(required = false) Boolean isPartner) {
        try {
            List<FuelStation> stations = fuelStationService.filterStations(province, district, brand, isOpen, isPartner);
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Filter failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Bulk create stations (for admin)
    @PostMapping("/bulk")
    public ResponseEntity<?> createStationsBulk(@Valid @RequestBody List<FuelStation> stations,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            // Set owner ID for all stations
            for (FuelStation station : stations) {
                station.setOwnerId(userId);
            }

            List<FuelStation> savedStations = fuelStationService.addStationsBulk(stations);

            Map<String, Object> response = new HashMap<>();
            response.put("message", savedStations.size() + " fuel stations created successfully");
            response.put("stations", savedStations);
            response.put("count", savedStations.size());
            response.put("success", true);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create stations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Get station statistics
    @GetMapping("/stats/summary")
    public ResponseEntity<?> getStationStatistics() {
        try {
            Map<String, Object> stats = fuelStationService.getStationStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Toggle station open/closed status
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStationStatus(@PathVariable Long id,
                                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            FuelStation station = fuelStationService.toggleOpenStatus(id, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Station status updated successfully");
            response.put("station", station);
            response.put("isOpen", station.getIsOpen());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Validate station fields
    private Map<String, String> validateStation(FuelStation station) {
        Map<String, String> errors = new HashMap<>();

        if (station.getName() == null || station.getName().trim().isEmpty()) {
            errors.put("name", "Station name is required");
        }
        if (station.getBrand() == null || station.getBrand().trim().isEmpty()) {
            errors.put("brand", "Brand is required");
        }
        if (station.getAddress() == null || station.getAddress().trim().isEmpty()) {
            errors.put("address", "Address is required");
        }
        if (station.getDistrict() == null || station.getDistrict().trim().isEmpty()) {
            errors.put("district", "District is required");
        }
        if (station.getProvince() == null || station.getProvince().trim().isEmpty()) {
            errors.put("province", "Province is required");
        }
        if (station.getLatitude() == null) {
            errors.put("latitude", "Latitude is required");
        } else if (station.getLatitude() < -90 || station.getLatitude() > 90) {
            errors.put("latitude", "Latitude must be between -90 and 90");
        }
        if (station.getLongitude() == null) {
            errors.put("longitude", "Longitude is required");
        } else if (station.getLongitude() < -180 || station.getLongitude() > 180) {
            errors.put("longitude", "Longitude must be between -180 and 180");
        }

        return errors;
    }
}