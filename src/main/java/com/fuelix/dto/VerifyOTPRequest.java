package com.fuelix.dto;

public class VerifyOTPRequest {
    private String identifier;
    private String otp;
    private String type; // MOBILE or EMAIL

    public VerifyOTPRequest() {}

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}