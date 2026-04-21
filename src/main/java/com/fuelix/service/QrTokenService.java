package com.fuelix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelix.model.QrPayload;
import com.fuelix.model.QrTokenInfo;
import com.fuelix.model.QrVerificationResult;
import com.fuelix.model.Vehicle;
import com.fuelix.repository.VehicleRepository;
import com.fuelix.util.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class QrTokenService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${qr.token.expiry.seconds:300}")
    private int tokenExpirySeconds;

    @Value("${qr.signature.secret}")
    private String signatureSecret;

    // In-memory storage
    private final Map<String, QrTokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> nonceStore = new ConcurrentHashMap<>();

    // Generate new QR token
    public QrTokenInfo generateToken(Long vehicleId, Long userId) throws Exception {
        String tokenId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(tokenExpirySeconds);

        QrTokenInfo token = new QrTokenInfo();
        token.setTokenId(tokenId);
        token.setNonce(nonce);
        token.setVehicleId(vehicleId);
        token.setUserId(userId);
        token.setCreatedAt(now);
        token.setExpiresAt(expiresAt);
        token.setUsed(false);

        // Store in memory
        tokenStore.put(tokenId, token);

        // Auto cleanup after expiry
        scheduleCleanup(tokenId, tokenExpirySeconds);

        return token;
    }

    private void scheduleCleanup(String tokenId, int delaySeconds) {
        new Thread(() -> {
            try {
                Thread.sleep(delaySeconds * 1000L);
                tokenStore.remove(tokenId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Generate QR payload with signature
    public String generateQrPayload(QrTokenInfo token, Vehicle vehicle) throws Exception {
        QrPayload payload = new QrPayload();
        payload.setPasscode(vehicle.getFuelPassCode());
        payload.setTokenId(token.getTokenId());
        payload.setNonce(token.getNonce());
        payload.setTimestamp(System.currentTimeMillis());

        String signatureData = payload.getPasscode() + "|" +
                payload.getTokenId() + "|" +
                payload.getNonce() + "|" +
                payload.getTimestamp();
        String signature = generateHmacSignature(signatureData);
        payload.setSignature(signature);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        String encryptedPayload = AESUtil.encrypt(jsonPayload);

        return String.format("FUELIX|2.0|%s|%s", encryptedPayload, signature);
    }

    // Validate QR code
    public QrVerificationResult validateQrCode(String qrData) {
        try {
            String[] parts = qrData.split("\\|");
            if (parts.length < 4 || !"FUELIX".equals(parts[0])) {
                return QrVerificationResult.invalid("Invalid QR format");
            }

            String encryptedPayload = parts[2];
            String receivedSignature = parts[3];

            String jsonPayload = AESUtil.decrypt(encryptedPayload);
            QrPayload payload = objectMapper.readValue(jsonPayload, QrPayload.class);

            String signatureData = payload.getPasscode() + "|" +
                    payload.getTokenId() + "|" +
                    payload.getNonce() + "|" +
                    payload.getTimestamp();
            String expectedSignature = generateHmacSignature(signatureData);

            if (!expectedSignature.equals(receivedSignature)) {
                return QrVerificationResult.invalid("Signature mismatch - QR may be tampered");
            }

            long age = System.currentTimeMillis() - payload.getTimestamp();
            if (age > 300000) {
                return QrVerificationResult.invalid("QR code expired (timestamp)");
            }

            // Check in-memory token
            QrTokenInfo token = tokenStore.get(payload.getTokenId());
            if (token == null) {
                return QrVerificationResult.invalid("Token not found or expired");
            }

            if (token.isUsed()) {
                return QrVerificationResult.invalid("QR code already used");
            }

            if (token.isExpired()) {
                tokenStore.remove(payload.getTokenId());
                return QrVerificationResult.invalid("Token expired");
            }

            // Check nonce
            if (nonceStore.containsKey(payload.getNonce())) {
                return QrVerificationResult.invalid("QR code already processed");
            }

            // Get vehicle - FIXED: Use Long.valueOf() to handle both Integer and Long
            Long vehicleId = convertToLong(token.getVehicleId());
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

            if (!vehicle.getFuelPassCode().equals(payload.getPasscode())) {
                return QrVerificationResult.invalid("Passcode mismatch");
            }

            // Mark nonce as used
            nonceStore.put(payload.getNonce(), "1");
            scheduleNonceCleanup(payload.getNonce(), tokenExpirySeconds);

            return QrVerificationResult.success(vehicle, payload.getTokenId());

        } catch (Exception e) {
            e.printStackTrace();
            return QrVerificationResult.invalid("Verification failed: " + e.getMessage());
        }
    }

    // Helper method to safely convert Object to Long
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Long");
    }

    // Helper method to safely convert Object to Boolean
    private Boolean convertToBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private void scheduleNonceCleanup(String nonce, int delaySeconds) {
        new Thread(() -> {
            try {
                Thread.sleep(delaySeconds * 1000L);
                nonceStore.remove(nonce);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Mark token as used
    public void markTokenAsUsed(String tokenId, Long staffId) {
        QrTokenInfo token = tokenStore.get(tokenId);
        if (token != null) {
            token.setUsed(true);
            token.setUsedBy(String.valueOf(staffId));
            token.setUsedAt(Instant.now());
        }
    }

    // Mark nonce as used
    public void markNonceAsUsed(String nonce) {
        nonceStore.put(nonce, "1");
        scheduleNonceCleanup(nonce, tokenExpirySeconds);
    }

    // Invalidate token
    public void invalidateToken(String tokenId) {
        tokenStore.remove(tokenId);
    }

    private String generateHmacSignature(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(signatureSecret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}