package com.fuelix.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String year;

    @Column(name = "registration_no", nullable = false, unique = true)
    private String registrationNo;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    @Column(name = "engine_cc")
    private String engineCC;

    private String color;

    @Column(name = "fuel_pass_code")
    @JsonIgnore
    private String fuelPassCode;

    @Column(name = "qr_generated_at")
    private LocalDateTime qrGeneratedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    @JsonProperty("fuelPassCode")
    private String decryptedFuelPassCode;

    public Vehicle() {}

    public Vehicle(Long userId, String type, String make, String model, String year,
                   String registrationNo, String fuelType, String engineCC, String color) {
        this.userId = userId;
        this.type = type;
        this.make = make;
        this.model = model;
        this.year = year;
        this.registrationNo = registrationNo;
        this.fuelType = fuelType;
        this.engineCC = engineCC;
        this.color = color;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getEngineCC() { return engineCC; }
    public void setEngineCC(String engineCC) { this.engineCC = engineCC; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    @JsonIgnore
    public String getFuelPassCode() { return fuelPassCode; }
    public void setFuelPassCode(String fuelPassCode) { this.fuelPassCode = fuelPassCode; }

    public LocalDateTime getQrGeneratedAt() { return qrGeneratedAt; }
    public void setQrGeneratedAt(LocalDateTime qrGeneratedAt) { this.qrGeneratedAt = qrGeneratedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDecryptedFuelPassCode() { return decryptedFuelPassCode; }
    public void setDecryptedFuelPassCode(String decryptedFuelPassCode) {
        this.decryptedFuelPassCode = decryptedFuelPassCode;
    }
}