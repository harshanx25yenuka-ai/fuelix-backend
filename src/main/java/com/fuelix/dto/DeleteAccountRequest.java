package com.fuelix.dto;

import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequest {

    @NotBlank(message = "NIC is required")
    private String nic;

    @NotBlank(message = "Reason is required")
    private String reason;

    public DeleteAccountRequest() {}

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}