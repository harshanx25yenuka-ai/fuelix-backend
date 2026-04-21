package com.fuelix.model;

import java.time.Instant;

public class QrTokenInfo {
    private String tokenId;
    private String nonce;
    private Long vehicleId;
    private Long userId;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean used;
    private String usedBy;
    private Instant usedAt;

    public QrTokenInfo() {}

    public QrTokenInfo(String tokenId, String nonce, Long vehicleId, Long userId,
                       Instant createdAt, Instant expiresAt, boolean used,
                       String usedBy, Instant usedAt) {
        this.tokenId = tokenId;
        this.nonce = nonce;
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
        this.usedBy = usedBy;
        this.usedAt = usedAt;
    }

    // Builder pattern without Lombok
    public static class Builder {
        private String tokenId;
        private String nonce;
        private Long vehicleId;
        private Long userId;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean used;
        private String usedBy;
        private Instant usedAt;

        public Builder tokenId(String tokenId) { this.tokenId = tokenId; return this; }
        public Builder nonce(String nonce) { this.nonce = nonce; return this; }
        public Builder vehicleId(Long vehicleId) { this.vehicleId = vehicleId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder used(boolean used) { this.used = used; return this; }
        public Builder usedBy(String usedBy) { this.usedBy = usedBy; return this; }
        public Builder usedAt(Instant usedAt) { this.usedAt = usedAt; return this; }

        public QrTokenInfo build() {
            return new QrTokenInfo(tokenId, nonce, vehicleId, userId, createdAt,
                    expiresAt, used, usedBy, usedAt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired() && !used;
    }

    // Getters and Setters
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public String getUsedBy() { return usedBy; }
    public void setUsedBy(String usedBy) { this.usedBy = usedBy; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
}