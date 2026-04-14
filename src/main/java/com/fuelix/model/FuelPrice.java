package com.fuelix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_prices")
public class FuelPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fuelType;

    @Column(name = "fuel_grade", nullable = false, unique = true)
    private String fuelGrade;

    @Column(name = "price_per_litre", nullable = false)
    private Double pricePerLitre;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public FuelPrice() {}

    public FuelPrice(String fuelType, String fuelGrade, Double pricePerLitre) {
        this.fuelType = fuelType;
        this.fuelGrade = fuelGrade;
        this.pricePerLitre = pricePerLitre;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getFuelGrade() { return fuelGrade; }
    public void setFuelGrade(String fuelGrade) { this.fuelGrade = fuelGrade; }

    public Double getPricePerLitre() { return pricePerLitre; }
    public void setPricePerLitre(Double pricePerLitre) { this.pricePerLitre = pricePerLitre; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}