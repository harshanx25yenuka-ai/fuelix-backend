package com.fuelix.model;

public class QrPayload {
    private String passcode;
    private String tokenId;
    private String nonce;
    private long timestamp;
    private String signature;
    private String tokenType;

    public QrPayload() {}

    public QrPayload(String passcode, String tokenId, String nonce, long timestamp, String signature, String tokenType) {
        this.passcode = passcode;
        this.tokenId = tokenId;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.signature = signature;
        this.tokenType = tokenType;
    }

    public static class Builder {
        private String passcode;
        private String tokenId;
        private String nonce;
        private long timestamp;
        private String signature;
        private String tokenType;

        public Builder passcode(String passcode) { this.passcode = passcode; return this; }
        public Builder tokenId(String tokenId) { this.tokenId = tokenId; return this; }
        public Builder nonce(String nonce) { this.nonce = nonce; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder signature(String signature) { this.signature = signature; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }

        public QrPayload build() {
            return new QrPayload(passcode, tokenId, nonce, timestamp, signature, tokenType);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public String getPasscode() { return passcode; }
    public void setPasscode(String passcode) { this.passcode = passcode; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}