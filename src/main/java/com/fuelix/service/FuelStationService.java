package com.fuelix.service;

import com.fuelix.model.FuelStation;
import com.fuelix.repository.FuelStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FuelStationService {

    @Autowired
    private FuelStationRepository fuelStationRepository;

    public List<FuelStation> getAllStations() {
        return fuelStationRepository.findAll();
    }

    public FuelStation getStationById(Long id) {
        return fuelStationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found with id: " + id));
    }

    public FuelStation getStationByOwnerId(Long ownerId) {
        return fuelStationRepository.findByOwnerId(ownerId).orElse(null);
    }

    public boolean userHasStation(Long ownerId) {
        return fuelStationRepository.existsByOwnerId(ownerId);
    }

    public List<FuelStation> getStationsByProvince(String province) {
        return fuelStationRepository.findByProvince(province);
    }

    public List<FuelStation> getStationsByDistrict(String district) {
        return fuelStationRepository.findByDistrict(district);
    }

    public List<FuelStation> getStationsByBrand(String brand) {
        return fuelStationRepository.findByBrand(brand);
    }

    public List<FuelStation> getPartnerStations() {
        return fuelStationRepository.findByIsFuelixPartnerTrue();
    }

    public List<FuelStation> getOpenStations() {
        return fuelStationRepository.findAllOpen();
    }

    @Transactional
    public FuelStation addStation(FuelStation station) {
        // Set default values if not provided
        if (station.getIsOpen() == null) {
            station.setIsOpen(true);
        }
        if (station.getIsFuelixPartner() == null) {
            station.setIsFuelixPartner(false);
        }
        if (station.getIs24Hours() == null) {
            station.setIs24Hours(false);
        }
        return fuelStationRepository.save(station);
    }

    @Transactional
    public FuelStation updateStation(Long id, FuelStation stationDetails, Long ownerId) {
        FuelStation station = getStationById(id);

        // Check if the station belongs to the user
        if (!station.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("You don't have permission to update this station");
        }

        station.setName(stationDetails.getName());
        station.setBrand(stationDetails.getBrand());
        station.setAddress(stationDetails.getAddress());
        station.setDistrict(stationDetails.getDistrict());
        station.setProvince(stationDetails.getProvince());
        station.setLatitude(stationDetails.getLatitude());
        station.setLongitude(stationDetails.getLongitude());
        station.setAvailableFuels(stationDetails.getAvailableFuels());
        station.setIsFuelixPartner(stationDetails.getIsFuelixPartner());
        station.setIs24Hours(stationDetails.getIs24Hours());
        station.setOperatingHours(stationDetails.getOperatingHours());
        station.setAmenities(stationDetails.getAmenities());
        station.setIsOpen(stationDetails.getIsOpen());
        return fuelStationRepository.save(station);
    }

    @Transactional
    public void deleteStation(Long id, Long ownerId) {
        FuelStation station = getStationById(id);

        // Check if the station belongs to the user
        if (!station.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("You don't have permission to delete this station");
        }

        fuelStationRepository.delete(station);
    }

    public List<FuelStation> searchStationsByName(String query) {
        return fuelStationRepository.searchByName(query);
    }

    public List<FuelStation> filterStations(String province, String district, String brand, Boolean isOpen, Boolean isPartner) {
        List<FuelStation> stations = fuelStationRepository.findAll();

        if (province != null && !province.isEmpty()) {
            stations = stations.stream()
                    .filter(s -> s.getProvince().equalsIgnoreCase(province))
                    .collect(Collectors.toList());
        }

        if (district != null && !district.isEmpty()) {
            stations = stations.stream()
                    .filter(s -> s.getDistrict().equalsIgnoreCase(district))
                    .collect(Collectors.toList());
        }

        if (brand != null && !brand.isEmpty()) {
            stations = stations.stream()
                    .filter(s -> s.getBrand().equalsIgnoreCase(brand))
                    .collect(Collectors.toList());
        }

        if (isOpen != null) {
            stations = stations.stream()
                    .filter(s -> s.getIsOpen().equals(isOpen))
                    .collect(Collectors.toList());
        }

        if (isPartner != null) {
            stations = stations.stream()
                    .filter(s -> s.getIsFuelixPartner().equals(isPartner))
                    .collect(Collectors.toList());
        }

        return stations;
    }

    @Transactional
    public List<FuelStation> addStationsBulk(List<FuelStation> stations) {
        List<FuelStation> savedStations = new ArrayList<>();
        for (FuelStation station : stations) {
            savedStations.add(addStation(station));
        }
        return savedStations;
    }

    public Map<String, Object> getStationStatistics() {
        List<FuelStation> allStations = fuelStationRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStations", allStations.size());
        stats.put("openStations", allStations.stream().filter(FuelStation::getIsOpen).count());
        stats.put("closedStations", allStations.stream().filter(s -> !s.getIsOpen()).count());
        stats.put("partnerStations", allStations.stream().filter(FuelStation::getIsFuelixPartner).count());
        stats.put("nonPartnerStations", allStations.stream().filter(s -> !s.getIsFuelixPartner()).count());
        stats.put("twentyFourHourStations", allStations.stream().filter(FuelStation::getIs24Hours).count());

        // Brand distribution
        Map<String, Long> brandDistribution = allStations.stream()
                .collect(Collectors.groupingBy(FuelStation::getBrand, Collectors.counting()));
        stats.put("brandDistribution", brandDistribution);

        // Province distribution
        Map<String, Long> provinceDistribution = allStations.stream()
                .collect(Collectors.groupingBy(FuelStation::getProvince, Collectors.counting()));
        stats.put("provinceDistribution", provinceDistribution);

        return stats;
    }

    @Transactional
    public FuelStation toggleOpenStatus(Long id, Long ownerId) {
        FuelStation station = getStationById(id);

        // Check if the station belongs to the user
        if (!station.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("You don't have permission to modify this station");
        }

        station.setIsOpen(!station.getIsOpen());
        return fuelStationRepository.save(station);
    }
}