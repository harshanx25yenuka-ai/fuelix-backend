package com.fuelix.service;

import com.fuelix.model.Vehicle;
import com.fuelix.repository.FuelLogRepository;
import com.fuelix.repository.QuotaRepository;
import com.fuelix.repository.VehicleRepository;
import com.fuelix.util.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FuelLogRepository fuelLogRepository;

    @Autowired
    private QuotaRepository quotaRepository;

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 12;
    private static final int MAX_ATTEMPTS = 100;

    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        for (Vehicle vehicle : vehicles) {
            if (vehicle.getFuelPassCode() != null && !vehicle.getFuelPassCode().isEmpty()) {
                try {
                    String decrypted = AESUtil.decrypt(vehicle.getFuelPassCode());
                    vehicle.setDecryptedFuelPassCode(decrypted);
                } catch (Exception e) {
                    System.err.println("Failed to decrypt fuel pass code for vehicle: " + vehicle.getId());
                }
            }
        }
        return vehicles;
    }

    public Vehicle addVehicle(Vehicle vehicle) {
        if (vehicle.getCreatedAt() == null) {
            vehicle.setCreatedAt(LocalDateTime.now());
        }

        if (vehicle.getRegistrationNo() == null || vehicle.getRegistrationNo().isEmpty()) {
            vehicle.setRegistrationNo("TEMP-" + System.currentTimeMillis());
        }

        if (vehicle.getFuelType() == null || vehicle.getFuelType().isEmpty()) {
            vehicle.setFuelType("Petrol");
        }

        if (vehicle.getYear() == null || vehicle.getYear().isEmpty()) {
            vehicle.setYear(String.valueOf(LocalDateTime.now().getYear()));
        }

        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getUserVehicles(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserId(userId);

        for (Vehicle vehicle : vehicles) {
            if (vehicle.getFuelPassCode() != null && !vehicle.getFuelPassCode().isEmpty()) {
                try {
                    String decrypted = AESUtil.decrypt(vehicle.getFuelPassCode());
                    vehicle.setDecryptedFuelPassCode(decrypted);
                } catch (Exception e) {
                    System.err.println("Failed to decrypt fuel pass code for vehicle: " + vehicle.getId());
                }
            }
        }
        return vehicles;
    }

    public Vehicle getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));

        if (vehicle.getFuelPassCode() != null && !vehicle.getFuelPassCode().isEmpty()) {
            try {
                String decrypted = AESUtil.decrypt(vehicle.getFuelPassCode());
                vehicle.setDecryptedFuelPassCode(decrypted);
            } catch (Exception e) {
                System.err.println("Failed to decrypt fuel pass code for vehicle: " + id);
            }
        }
        return vehicle;
    }

    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setType(vehicleDetails.getType());
        vehicle.setMake(vehicleDetails.getMake());
        vehicle.setModel(vehicleDetails.getModel());
        vehicle.setYear(vehicleDetails.getYear());
        vehicle.setRegistrationNo(vehicleDetails.getRegistrationNo());
        vehicle.setFuelType(vehicleDetails.getFuelType());
        vehicle.setEngineCC(vehicleDetails.getEngineCC());
        vehicle.setColor(vehicleDetails.getColor());

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        List<com.fuelix.model.FuelLog> fuelLogs = fuelLogRepository.findByVehicleIdOrderByLoggedAtDesc(id);
        if (!fuelLogs.isEmpty()) {
            fuelLogRepository.deleteAll(fuelLogs);
            System.out.println("Deleted " + fuelLogs.size() + " fuel logs for vehicle ID: " + id);
        }

        List<com.fuelix.model.Quota> quotas = quotaRepository.findByVehicleIdOrderByWeekStartDesc(id);
        if (!quotas.isEmpty()) {
            quotaRepository.deleteAll(quotas);
            System.out.println("Deleted " + quotas.size() + " quota records for vehicle ID: " + id);
        }

        vehicleRepository.deleteById(id);
        System.out.println("Deleted vehicle ID: " + id);
    }

    @Transactional
    public Vehicle generateFuelPass(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (vehicle.getFuelPassCode() != null && !vehicle.getFuelPassCode().isEmpty()) {
            throw new RuntimeException("Fuel Pass already generated for this vehicle. Use regenerate endpoint to create a new one.");
        }

        String rawCode = generateUniquePassCode();

        try {
            String encryptedCode = AESUtil.encrypt(rawCode);
            vehicle.setFuelPassCode(encryptedCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt Fuel Pass Code: " + e.getMessage());
        }

        vehicle.setQrGeneratedAt(LocalDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);
        saved.setDecryptedFuelPassCode(rawCode);

        return saved;
    }

    @Transactional
    public Vehicle regenerateFuelPass(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        String rawCode = generateUniquePassCode();

        try {
            String encryptedCode = AESUtil.encrypt(rawCode);
            vehicle.setFuelPassCode(encryptedCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt Fuel Pass Code: " + e.getMessage());
        }

        vehicle.setQrGeneratedAt(LocalDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);
        saved.setDecryptedFuelPassCode(rawCode);

        return saved;
    }

    private String generateUniquePassCode() {
        Random random = new Random();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateSinglePassCode(random);

            if (!isPasscodeUsed(code)) {
                return code;
            }
        }

        throw new RuntimeException("Unable to generate unique Fuel Pass Code after " + MAX_ATTEMPTS + " attempts");
    }

    private String generateSinglePassCode(Random random) {
        List<Character> availableChars = CODE_CHARS.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());

        Collections.shuffle(availableChars, random);

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH && i < availableChars.size(); i++) {
            code.append(availableChars.get(i));
        }

        return code.toString();
    }

    private boolean isPasscodeUsed(String rawCode) {
        List<Vehicle> allVehicles = vehicleRepository.findAll();

        for (Vehicle v : allVehicles) {
            if (v.getFuelPassCode() != null && !v.getFuelPassCode().isEmpty()) {
                try {
                    String decrypted = AESUtil.decrypt(v.getFuelPassCode());
                    if (decrypted.equals(rawCode)) {
                        return true;
                    }
                } catch (Exception e) {
                    // Skip if decryption fails
                }
            }
        }
        return false;
    }

    public Vehicle getVehicleByPassCode(String passCode) {
        if (passCode == null || passCode.isEmpty()) {
            throw new RuntimeException("Passcode cannot be empty");
        }

        List<Vehicle> allVehicles = vehicleRepository.findAll();

        for (Vehicle vehicle : allVehicles) {
            if (vehicle.getFuelPassCode() != null && !vehicle.getFuelPassCode().isEmpty()) {
                try {
                    String decrypted = AESUtil.decrypt(vehicle.getFuelPassCode());
                    if (decrypted.equals(passCode)) {
                        vehicle.setDecryptedFuelPassCode(decrypted);
                        return vehicle;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to decrypt for vehicle: " + vehicle.getId() + " - " + e.getMessage());
                }
            }
        }

        throw new RuntimeException("Invalid Fuel Pass Code");
    }

    public boolean isPasscodeValid(String passCode) {
        try {
            getVehicleByPassCode(passCode);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public Vehicle getVehicleByRegistrationNo(String registrationNo) {
        return vehicleRepository.findByRegistrationNo(registrationNo)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with registration: " + registrationNo));
    }

    public boolean registrationNoExists(String registrationNo, Long userId) {
        return vehicleRepository.existsByRegistrationNoAndUserId(registrationNo, userId);
    }
}