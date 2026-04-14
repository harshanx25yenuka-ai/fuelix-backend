package com.fuelix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StaffRequest {

    @NotBlank(message = "NIC is required")
    private String nic;

    @NotNull(message = "Station ID is required")
    private Long stationId;

    public StaffRequest() {}

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
}