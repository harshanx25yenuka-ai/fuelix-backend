package com.fuelix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_logs")
public class FuelLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(nullable = false)
    private Double litres;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    @Column(name = "fuel_grade", nullable = false)
    private String fuelGrade;

    @Column(name = "price_per_litre", nullable = false)
    private Double pricePerLitre;

    @Column(name = "total_cost", nullable = false)
    private Double totalCost;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    public FuelLog() {}

    public FuelLog(Long userId, Long vehicleId, Double litres, String fuelType, String fuelGrade,
                   Double pricePerLitre, Double totalCost, String stationName, LocalDateTime loggedAt) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.litres = litres;
        this.fuelType = fuelType;
        this.fuelGrade = fuelGrade;
        this.pricePerLitre = pricePerLitre;
        this.totalCost = totalCost;
        this.stationName = stationName;
        this.loggedAt = loggedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Double getLitres() { return litres; }
    public void setLitres(Double litres) { this.litres = litres; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getFuelGrade() { return fuelGrade; }
    public void setFuelGrade(String fuelGrade) { this.fuelGrade = fuelGrade; }

    public Double getPricePerLitre() { return pricePerLitre; }
    public void setPricePerLitre(Double pricePerLitre) { this.pricePerLitre = pricePerLitre; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}