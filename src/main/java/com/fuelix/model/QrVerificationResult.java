package com.fuelix.model;

public class QrVerificationResult {
    private boolean valid;
    private String error;
    private Vehicle vehicle;
    private String tokenId;

    public QrVerificationResult() {}

    public QrVerificationResult(boolean valid, String error, Vehicle vehicle, String tokenId) {
        this.valid = valid;
        this.error = error;
        this.vehicle = vehicle;
        this.tokenId = tokenId;
    }

    public static QrVerificationResult success(Vehicle vehicle, String tokenId) {
        return new QrVerificationResult(true, null, vehicle, tokenId);
    }

    public static QrVerificationResult invalid(String error) {
        return new QrVerificationResult(false, error, null, null);
    }

    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
}