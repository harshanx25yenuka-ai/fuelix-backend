package com.fuelix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelix.model.QrPayload;
import com.fuelix.model.QrTokenInfo;
import com.fuelix.model.QrVerificationResult;
import com.fuelix.model.Vehicle;
import com.fuelix.model.SharedVehicle;
import com.fuelix.repository.VehicleRepository;
import com.fuelix.repository.SharedVehicleRepository;
import com.fuelix.util.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
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
    private SharedVehicleRepository sharedVehicleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${qr.token.expiry.seconds:300}")
    private int tokenExpirySeconds;

    @Value("${qr.signature.secret}")
    private String signatureSecret;

    private final Map<String, QrTokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> nonceStore = new ConcurrentHashMap<>();

    // Generate token for OWNER
    public QrTokenInfo generateToken(Long vehicleId, Long userId) throws Exception {
        String tokenId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(tokenExpirySeconds);

        QrTokenInfo token = QrTokenInfo.builder()
                .tokenId(tokenId)
                .nonce(nonce)
                .vehicleId(vehicleId)
                .userId(userId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .used(false)
                .tokenType("OWNER")
                .build();

        tokenStore.put(tokenId, token);
        scheduleCleanup(tokenId, tokenExpirySeconds);

        return token;
    }

    // Generate token for SHARED USER (with permission check)
    public QrTokenInfo generateSharedToken(Long vehicleId, Long sharedWithUserId, Long ownerId) throws Exception {
        // Check if vehicle is shared with this user
        SharedVehicle shared = sharedVehicleRepository
                .findByVehicleIdAndSharedWithUserId(vehicleId, sharedWithUserId)
                .orElseThrow(() -> new RuntimeException("Vehicle not shared with this user"));

        // Check refuel permission from database (SECURITY CHECK)
        Map<String, Boolean> permissions = parseJsonToMap(shared.getPermissions());
        if (!permissions.getOrDefault("can_refuel", false)) {
            throw new RuntimeException("PERMISSION_DENIED: You don't have permission to refuel this vehicle");
        }

        String tokenId = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(tokenExpirySeconds);

        QrTokenInfo token = QrTokenInfo.builder()
                .tokenId(tokenId)
                .nonce(nonce)
                .vehicleId(vehicleId)
                .userId(sharedWithUserId)
                .ownerId(ownerId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .used(false)
                .tokenType("SHARED")
                .build();

        tokenStore.put(tokenId, token);
        scheduleCleanup(tokenId, tokenExpirySeconds);

        return token;
    }

    // Generate QR payload with signature
    public String generateQrPayload(QrTokenInfo token, Vehicle vehicle) throws Exception {
        QrPayload payload = QrPayload.builder()
                .passcode(vehicle.getFuelPassCode())
                .tokenId(token.getTokenId())
                .nonce(token.getNonce())
                .timestamp(System.currentTimeMillis())
                .tokenType(token.getTokenType())
                .build();

        String signatureData = payload.getPasscode() + "|" +
                payload.getTokenId() + "|" +
                payload.getNonce() + "|" +
                payload.getTimestamp() + "|" +
                payload.getTokenType();
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
                    payload.getTimestamp() + "|" +
                    payload.getTokenType();
            String expectedSignature = generateHmacSignature(signatureData);

            if (!expectedSignature.equals(receivedSignature)) {
                return QrVerificationResult.invalid("Signature mismatch - QR may be tampered");
            }

            long age = System.currentTimeMillis() - payload.getTimestamp();
            if (age > 300000) {
                return QrVerificationResult.invalid("QR code expired (timestamp)");
            }

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

            if (nonceStore.containsKey(payload.getNonce())) {
                return QrVerificationResult.invalid("QR code already processed");
            }

            Vehicle vehicle = vehicleRepository.findById(token.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

            if (!vehicle.getFuelPassCode().equals(payload.getPasscode())) {
                return QrVerificationResult.invalid("Passcode mismatch");
            }

            nonceStore.put(payload.getNonce(), "1");
            scheduleNonceCleanup(payload.getNonce(), tokenExpirySeconds);

            return QrVerificationResult.success(vehicle, payload.getTokenId());

        } catch (Exception e) {
            e.printStackTrace();
            return QrVerificationResult.invalid("Verification failed: " + e.getMessage());
        }
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

    // Get token info
    public QrTokenInfo getToken(String tokenId) {
        return tokenStore.get(tokenId);
    }

    // Invalidate token
    public void invalidateToken(String tokenId) {
        tokenStore.remove(tokenId);
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

    private Map<String, Boolean> parseJsonToMap(String json) {
        Map<String, Boolean> map = new HashMap<>();
        if (json == null || json.isEmpty()) return map;

        try {
            String clean = json.replace("{", "").replace("}", "");
            if (clean.isEmpty()) return map;

            for (String pair : clean.split(",")) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    String key = parts[0].replace("\"", "").trim();
                    Boolean value = Boolean.parseBoolean(parts[1].trim());
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            // Use default
        }
        return map;
    }

    private String generateHmacSignature(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(signatureSecret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}