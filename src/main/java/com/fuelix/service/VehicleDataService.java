package com.fuelix.service;

import com.fuelix.model.VehicleBrand;
import com.fuelix.model.VehicleModel;
import com.fuelix.repository.VehicleBrandRepository;
import com.fuelix.repository.VehicleModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VehicleDataService {

    @Autowired
    private VehicleBrandRepository vehicleBrandRepository;

    @Autowired
    private VehicleModelRepository vehicleModelRepository;

    public List<VehicleBrand> getAllBrands() {
        return vehicleBrandRepository.findAll();
    }

    @Transactional
    public VehicleBrand addBrand(String brandName) {
        if (vehicleBrandRepository.existsByBrandName(brandName)) {
            throw new RuntimeException("Brand already exists: " + brandName);
        }
        VehicleBrand brand = new VehicleBrand(brandName);
        return vehicleBrandRepository.save(brand);
    }

    @Transactional
    public void deleteBrand(Long id) {
        VehicleBrand brand = vehicleBrandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        // Delete all models under this brand
        List<VehicleModel> models = vehicleModelRepository.findByBrandIdOrderByModelNameAsc(id);
        if (!models.isEmpty()) {
            vehicleModelRepository.deleteAll(models);
        }

        vehicleBrandRepository.deleteById(id);
    }

    public List<VehicleModel> getModelsByBrandAndType(Long brandId, String vehicleType) {
        return vehicleModelRepository.findByBrandIdAndVehicleTypeOrderByModelNameAsc(brandId, vehicleType);
    }

    @Transactional
    public VehicleModel addModel(Long brandId, String modelName, String vehicleType) {
        if (!vehicleBrandRepository.existsById(brandId)) {
            throw new RuntimeException("Brand not found with id: " + brandId);
        }

        if (vehicleModelRepository.existsByBrandIdAndModelNameAndVehicleType(brandId, modelName, vehicleType)) {
            throw new RuntimeException("Model '" + modelName + "' already exists for this brand and vehicle type");
        }

        VehicleModel model = new VehicleModel(brandId, modelName, vehicleType);
        return vehicleModelRepository.save(model);
    }

    @Transactional
    public void deleteModel(Long id) {
        if (!vehicleModelRepository.existsById(id)) {
            throw new RuntimeException("Model not found");
        }
        vehicleModelRepository.deleteById(id);
    }

    public Map<String, List<String>> getBrandsWithModelsByType(String vehicleType) {
        List<VehicleBrand> brands = vehicleBrandRepository.findAll();
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (VehicleBrand brand : brands) {
            List<String> models = vehicleModelRepository.findByBrandIdAndVehicleTypeOrderByModelNameAsc(brand.getId(), vehicleType)
                    .stream()
                    .map(VehicleModel::getModelName)
                    .collect(Collectors.toList());
            if (!models.isEmpty()) {
                result.put(brand.getBrandName(), models);
            }
        }
        return result;
    }

    @Transactional
    public void initializeDefaultVehicleData() {
        String[] defaultBrands = {
                "Toyota", "Honda", "Suzuki", "Nissan", "Mitsubishi", "Mazda",
                "Hyundai", "Kia", "BMW", "Mercedes-Benz", "Volkswagen", "Ford",
                "Bajaj", "TVS", "Hero", "Yamaha", "Kawasaki", "Tata", "Ashok Leyland"
        };

        Map<String, Long> brandIdMap = new HashMap<>();

        for (String brandName : defaultBrands) {
            VehicleBrand brand;
            if (!vehicleBrandRepository.existsByBrandName(brandName)) {
                brand = vehicleBrandRepository.save(new VehicleBrand(brandName));
            } else {
                brand = vehicleBrandRepository.findByBrandName(brandName).get();
            }
            brandIdMap.put(brandName, brand.getId());
        }

        Object[][] modelsData = {
                {"Toyota", "Camry", "Car"}, {"Toyota", "Corolla", "Car"}, {"Toyota", "Prius", "Car"},
                {"Toyota", "RAV4", "Car"}, {"Toyota", "Yaris", "Car"}, {"Toyota", "Vios", "Car"},
                {"Honda", "Civic", "Car"}, {"Honda", "Accord", "Car"}, {"Honda", "CR-V", "Car"},
                {"Honda", "City", "Car"}, {"Honda", "Fit", "Car"},
                {"Suzuki", "Swift", "Car"}, {"Suzuki", "Alto", "Car"}, {"Suzuki", "Wagon R", "Car"},
                {"Nissan", "Sunny", "Car"}, {"Nissan", "Leaf", "Car"}, {"Nissan", "X-Trail", "Car"},
                {"Hyundai", "Elantra", "Car"}, {"Hyundai", "Sonata", "Car"}, {"Hyundai", "Tucson", "Car"},
                {"Kia", "Cerato", "Car"}, {"Kia", "Sportage", "Car"}, {"Kia", "Sorento", "Car"},
                {"Yamaha", "FZ", "Motorcycle"}, {"Yamaha", "R15", "Motorcycle"}, {"Yamaha", "MT-15", "Motorcycle"},
                {"Yamaha", "RayZR", "Motorcycle"}, {"Suzuki", "Gixxer", "Motorcycle"}, {"Suzuki", "Access", "Motorcycle"},
                {"Honda", "CBR", "Motorcycle"}, {"Honda", "Dio", "Motorcycle"}, {"Honda", "Activa", "Motorcycle"},
                {"Hero", "Splendor", "Motorcycle"}, {"Hero", "Passion", "Motorcycle"}, {"Hero", "HF Deluxe", "Motorcycle"},
                {"Bajaj", "Pulsar", "Motorcycle"}, {"Bajaj", "Discover", "Motorcycle"}, {"Bajaj", "Platina", "Motorcycle"},
                {"Toyota", "Hiace", "Van"}, {"Nissan", "NV200", "Van"}, {"Suzuki", "Every", "Van"},
                {"Mitsubishi", "Delica", "Van"}, {"Hyundai", "H-1", "Van"},
                {"Tata", "Ace", "Truck"}, {"Tata", "Super Ace", "Truck"}, {"Tata", "Ultra", "Truck"},
                {"Ashok Leyland", "Dost", "Truck"}, {"Ashok Leyland", "Partner", "Truck"},
                {"Mitsubishi", "Canter", "Truck"},
                {"Ashok Leyland", "Viking", "Bus"}, {"Ashok Leyland", "Lynx", "Bus"},
                {"Tata", "Starbus", "Bus"}, {"Toyota", "Coaster", "Bus"},
                {"Bajaj", "RE", "Three-Wheeler"}, {"Bajaj", "Compact", "Three-Wheeler"},
                {"Bajaj", "Maxima", "Three-Wheeler"}, {"TVS", "King", "Three-Wheeler"}
        };

        for (Object[] data : modelsData) {
            String brandName = (String) data[0];
            String modelName = (String) data[1];
            String vehicleType = (String) data[2];
            Long brandId = brandIdMap.get(brandName);

            if (brandId != null && !vehicleModelRepository.existsByBrandIdAndModelNameAndVehicleType(brandId, modelName, vehicleType)) {
                vehicleModelRepository.save(new VehicleModel(brandId, modelName, vehicleType));
            }
        }
    }
}