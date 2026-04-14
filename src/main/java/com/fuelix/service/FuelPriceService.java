package com.fuelix.service;

import com.fuelix.model.FuelPrice;
import com.fuelix.repository.FuelPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FuelPriceService {

    @Autowired
    private FuelPriceRepository fuelPriceRepository;

    public List<FuelPrice> getAllFuelPrices() {
        return fuelPriceRepository.findAllByOrderByFuelTypeAscFuelGradeAsc();
    }

    public FuelPrice getFuelPriceById(Long id) {
        return fuelPriceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fuel price not found"));
    }

    public FuelPrice getFuelPriceByGrade(String fuelGrade) {
        return fuelPriceRepository.findByFuelGrade(fuelGrade)
                .orElseThrow(() -> new RuntimeException("Fuel price not found for grade: " + fuelGrade));
    }

    @Transactional
    public FuelPrice updateFuelPrice(Long id, Double newPrice, String updatedBy) {
        FuelPrice fuelPrice = getFuelPriceById(id);
        fuelPrice.setPricePerLitre(newPrice);
        fuelPrice.setUpdatedAt(LocalDateTime.now());
        fuelPrice.setUpdatedBy(updatedBy);
        return fuelPriceRepository.save(fuelPrice);
    }

    @Transactional
    public List<FuelPrice> bulkUpdateFuelPrices(List<FuelPriceUpdateRequest> updates, String updatedBy) {
        List<FuelPrice> updatedPrices = new java.util.ArrayList<>();

        for (FuelPriceUpdateRequest update : updates) {
            FuelPrice fuelPrice = getFuelPriceById(update.getId());
            fuelPrice.setPricePerLitre(update.getPricePerLitre());
            fuelPrice.setUpdatedAt(LocalDateTime.now());
            fuelPrice.setUpdatedBy(updatedBy);
            updatedPrices.add(fuelPriceRepository.save(fuelPrice));
        }

        return updatedPrices;
    }

    @Transactional
    public void initializeDefaultFuelPrices() {
        Object[][] defaultPrices = {
                {"Petrol", "Petrol 92", 317.0},
                {"Petrol", "Petrol 95", 365.0},
                {"Diesel", "Auto Diesel", 303.0},
                {"Diesel", "Super Diesel", 353.0},
                {"Kerosene", "Kerosene", 195.0}
        };

        for (Object[] price : defaultPrices) {
            String fuelType = (String) price[0];
            String fuelGrade = (String) price[1];
            Double pricePerLitre = (Double) price[2];

            if (!fuelPriceRepository.existsByFuelGrade(fuelGrade)) {
                FuelPrice fuelPrice = new FuelPrice(fuelType, fuelGrade, pricePerLitre);
                fuelPrice.setUpdatedBy("system");
                fuelPriceRepository.save(fuelPrice);
            }
        }
    }

    public static class FuelPriceUpdateRequest {
        private Long id;
        private Double pricePerLitre;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Double getPricePerLitre() { return pricePerLitre; }
        public void setPricePerLitre(Double pricePerLitre) { this.pricePerLitre = pricePerLitre; }
    }
}