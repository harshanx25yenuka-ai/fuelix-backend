package com.fuelix.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Entity
@Table(name = "fuel_stations")
public class FuelStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "available_fuels", columnDefinition = "JSON")
    private String availableFuels;

    @Column(name = "is_fuelix_partner")
    private Boolean isFuelixPartner = false;

    @Column(name = "is_24_hours")
    private Boolean is24Hours = false;

    @Column(name = "operating_hours")
    private String operatingHours;

    @Column(name = "amenities", columnDefinition = "JSON")
    private String amenities;

    @Column(name = "is_open")
    private Boolean isOpen = true;

    @Column(name = "owner_id")
    private Long ownerId;

    public FuelStation() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAvailableFuels() { return availableFuels; }
    public void setAvailableFuels(String availableFuels) { this.availableFuels = availableFuels; }

    public Boolean getIsFuelixPartner() { return isFuelixPartner; }
    public void setIsFuelixPartner(Boolean isFuelixPartner) { this.isFuelixPartner = isFuelixPartner; }

    public Boolean getIs24Hours() { return is24Hours; }
    public void setIs24Hours(Boolean is24Hours) { this.is24Hours = is24Hours; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}