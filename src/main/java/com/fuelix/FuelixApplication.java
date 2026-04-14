package com.fuelix;

import com.fuelix.repository.QuotaLimitRepository;
import com.fuelix.service.FuelPriceService;
import com.fuelix.service.VehicleDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FuelixApplication {

    @Autowired
    private QuotaLimitRepository quotaLimitRepository;

    @Autowired
    private FuelPriceService fuelPriceService;

    @Autowired
    private VehicleDataService vehicleDataService;

    public static void main(String[] args) {
        SpringApplication.run(FuelixApplication.class, args);
    }

    @Bean
    public CommandLineRunner initializeDatabase() {
        return args -> {
            System.out.println("🔧 Starting database initialization...");

            // Initialize quota limits if empty
            String[] vehicleTypes = {"Car", "Van", "Motorcycle", "Truck", "Bus", "Three-Wheeler"};
            Double[] defaultQuotas = {25.0, 25.0, 2.0, 20.0, 45.0, 15.0};

            for (int i = 0; i < vehicleTypes.length; i++) {
                if (!quotaLimitRepository.existsByVehicleType(vehicleTypes[i])) {
                    com.fuelix.model.QuotaLimit limit = new com.fuelix.model.QuotaLimit(vehicleTypes[i], defaultQuotas[i]);
                    limit.setUpdatedBy("system");
                    quotaLimitRepository.save(limit);
                    System.out.println("✅ Initialized quota limit for: " + vehicleTypes[i]);
                } else {
                    System.out.println("⚠️ Quota limit already exists for: " + vehicleTypes[i]);
                }
            }

            // Initialize fuel prices if empty
            try {
                fuelPriceService.initializeDefaultFuelPrices();
                System.out.println("✅ Initialized default fuel prices");
            } catch (Exception e) {
                System.out.println("⚠️ Fuel prices may already exist: " + e.getMessage());
            }

            // Initialize vehicle brands and models
            try {
                vehicleDataService.initializeDefaultVehicleData();
                System.out.println("✅ Initialized vehicle brands and models");
            } catch (Exception e) {
                System.out.println("⚠️ Vehicle data may already exist: " + e.getMessage());
            }

            System.out.println("🎉 Database initialization complete!");
        };
    }
}