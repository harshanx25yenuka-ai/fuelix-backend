package com.fuelix.dto;

public class QrGenerationResponse {
    private String qrData;
    private String tokenId;
    private int expiresIn;
    private long generatedAt;
    private String message;

    public QrGenerationResponse() {}

    public QrGenerationResponse(String qrData, String tokenId, int expiresIn, long generatedAt, String message) {
        this.qrData = qrData;
        this.tokenId = tokenId;
        this.expiresIn = expiresIn;
        this.generatedAt = generatedAt;
        this.message = message;
    }

    // Builder pattern
    public static class Builder {
        private String qrData;
        private String tokenId;
        private int expiresIn;
        private long generatedAt;
        private String message;

        public Builder qrData(String qrData) { this.qrData = qrData; return this; }
        public Builder tokenId(String tokenId) { this.tokenId = tokenId; return this; }
        public Builder expiresIn(int expiresIn) { this.expiresIn = expiresIn; return this; }
        public Builder generatedAt(long generatedAt) { this.generatedAt = generatedAt; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public QrGenerationResponse build() {
            return new QrGenerationResponse(qrData, tokenId, expiresIn, generatedAt, message);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public String getQrData() { return qrData; }
    public void setQrData(String qrData) { this.qrData = qrData; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}