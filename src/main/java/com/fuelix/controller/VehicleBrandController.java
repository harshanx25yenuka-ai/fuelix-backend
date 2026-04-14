package com.fuelix.controller;

import com.fuelix.model.VehicleBrand;
import com.fuelix.model.VehicleModel;
import com.fuelix.service.VehicleDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle-data")
public class VehicleBrandController {

    @Autowired
    private VehicleDataService vehicleDataService;

    @GetMapping("/brands")
    public ResponseEntity<?> getAllBrands() {
        List<VehicleBrand> brands = vehicleDataService.getAllBrands();
        return ResponseEntity.ok(brands);
    }

    @PostMapping("/brands")
    public ResponseEntity<?> addBrand(@RequestBody Map<String, String> request) {
        try {
            String brandName = request.get("brandName");
            if (brandName == null || brandName.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Brand name is required");
                return ResponseEntity.badRequest().body(error);
            }
            VehicleBrand brand = vehicleDataService.addBrand(brandName.trim());
            return ResponseEntity.ok(brand);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        try {
            vehicleDataService.deleteBrand(id);
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Brand deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/models/brand/{brandId}/type/{vehicleType}")
    public ResponseEntity<?> getModelsByBrandAndType(@PathVariable Long brandId, @PathVariable String vehicleType) {
        List<VehicleModel> models = vehicleDataService.getModelsByBrandAndType(brandId, vehicleType);
        return ResponseEntity.ok(models);
    }

    @PostMapping("/models")
    public ResponseEntity<?> addModel(@RequestBody Map<String, Object> request) {
        try {
            Long brandId = Long.valueOf(request.get("brandId").toString());
            String modelName = request.get("modelName").toString();
            String vehicleType = request.get("vehicleType").toString();

            if (modelName == null || modelName.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Model name is required");
                return ResponseEntity.badRequest().body(error);
            }

            VehicleModel model = vehicleDataService.addModel(brandId, modelName.trim(), vehicleType);
            return ResponseEntity.ok(model);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @DeleteMapping("/models/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable Long id) {
        try {
            vehicleDataService.deleteModel(id);
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Model deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/brands-with-models/type/{vehicleType}")
    public ResponseEntity<?> getBrandsWithModelsByType(@PathVariable String vehicleType) {
        Map<String, List<String>> data = vehicleDataService.getBrandsWithModelsByType(vehicleType);
        return ResponseEntity.ok(data);
    }
}