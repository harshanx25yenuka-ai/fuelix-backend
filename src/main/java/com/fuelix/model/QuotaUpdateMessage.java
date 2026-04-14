package com.fuelix.model;

public class QuotaUpdateMessage {
    private String vehicleType;
    private Double oldQuota;
    private Double newQuota;
    private String updatedBy;
    private String updatedAt;

    public QuotaUpdateMessage() {}

    public QuotaUpdateMessage(String vehicleType, Double oldQuota, Double newQuota, String updatedBy, String updatedAt) {
        this.vehicleType = vehicleType;
        this.oldQuota = oldQuota;
        this.newQuota = newQuota;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Double getOldQuota() { return oldQuota; }
    public void setOldQuota(Double oldQuota) { this.oldQuota = oldQuota; }

    public Double getNewQuota() { return newQuota; }
    public void setNewQuota(Double newQuota) { this.newQuota = newQuota; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}