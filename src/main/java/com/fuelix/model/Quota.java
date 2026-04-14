package com.fuelix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_quotas")
public class Quota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "week_start", nullable = false)
    private LocalDateTime weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDateTime weekEnd;

    @Column(name = "quota_litres", nullable = false)
    private Double quotaLitres;

    @Column(name = "used_litres", nullable = false)
    private Double usedLitres;

    public Quota() {}

    public Quota(Long vehicleId, LocalDateTime weekStart, LocalDateTime weekEnd, Double quotaLitres, Double usedLitres) {
        this.vehicleId = vehicleId;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.quotaLitres = quotaLitres;
        this.usedLitres = usedLitres;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public LocalDateTime getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDateTime weekStart) { this.weekStart = weekStart; }

    public LocalDateTime getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDateTime weekEnd) { this.weekEnd = weekEnd; }

    public Double getQuotaLitres() { return quotaLitres; }
    public void setQuotaLitres(Double quotaLitres) { this.quotaLitres = quotaLitres; }

    public Double getUsedLitres() { return usedLitres; }
    public void setUsedLitres(Double usedLitres) { this.usedLitres = usedLitres; }

    public Double getRemainingLitres() {
        return Math.max(0, quotaLitres - usedLitres);
    }
}