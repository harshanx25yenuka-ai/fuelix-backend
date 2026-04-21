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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class QrTokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${qr.token.expiry.seconds:300}")
    private int tokenExpirySeconds;

    @Value("${qr.signature.secret}")
    private String signatureSecret;

    private static final String TOKEN_KEY_PREFIX = "qr:token:";
    private static final String USED_NONCE_PREFIX = "qr:nonce:";

    // Generate new QR token
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
                .build();

        // Store in Redis with TTL
        String key = TOKEN_KEY_PREFIX + tokenId;
        redisTemplate.opsForHash().put(key, "tokenId", tokenId);
        redisTemplate.opsForHash().put(key, "nonce", nonce);
        redisTemplate.opsForHash().put(key, "vehicleId", vehicleId);
        redisTemplate.opsForHash().put(key, "userId", userId);
        redisTemplate.opsForHash().put(key, "createdAt", token.getCreatedAt().toString());
        redisTemplate.opsForHash().put(key, "expiresAt", token.getExpiresAt().toString());
        redisTemplate.opsForHash().put(key, "used", false);
        redisTemplate.expire(key, tokenExpirySeconds, TimeUnit.SECONDS);

        return token;
    }

    // Generate QR payload with signature
    public String generateQrPayload(QrTokenInfo token, Vehicle vehicle) throws Exception {
        // Create payload
        QrPayload payload = QrPayload.builder()
                .passcode(vehicle.getFuelPassCode())
                .tokenId(token.getTokenId())
                .nonce(token.getNonce())
                .timestamp(System.currentTimeMillis())
                .build();

        // Generate signature
        String signatureData = payload.getPasscode() + "|" +
                payload.getTokenId() + "|" +
                payload.getNonce() + "|" +
                payload.getTimestamp();
        String signature = generateHmacSignature(signatureData);
        payload.setSignature(signature);

        // Convert to JSON and encrypt
        String jsonPayload = objectMapper.writeValueAsString(payload);
        String encryptedPayload = AESUtil.encrypt(jsonPayload);

        // Build final QR string
        return String.format("FUELIX|2.0|%s|%s", encryptedPayload, signature);
    }

    // Validate QR code
    public QrVerificationResult validateQrCode(String qrData) {
        try {
            // Parse QR data
            String[] parts = qrData.split("\\|");
            if (parts.length < 4 || !"FUELIX".equals(parts[0])) {
                return QrVerificationResult.invalid("Invalid QR format");
            }

            String encryptedPayload = parts[2];
            String receivedSignature = parts[3];

            // Decrypt payload
            String jsonPayload = AESUtil.decrypt(encryptedPayload);
            QrPayload payload = objectMapper.readValue(jsonPayload, QrPayload.class);

            // Verify signature
            String signatureData = payload.getPasscode() + "|" +
                    payload.getTokenId() + "|" +
                    payload.getNonce() + "|" +
                    payload.getTimestamp();
            String expectedSignature = generateHmacSignature(signatureData);

            if (!expectedSignature.equals(receivedSignature)) {
                return QrVerificationResult.invalid("Signature mismatch - QR may be tampered");
            }

            // Check timestamp (prevent very old QRs)
            long age = System.currentTimeMillis() - payload.getTimestamp();
            if (age > 300000) {
                return QrVerificationResult.invalid("QR code expired (timestamp)");
            }

            // Validate token in Redis
            String key = TOKEN_KEY_PREFIX + payload.getTokenId();
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                return QrVerificationResult.invalid("Token not found or expired");
            }

            // Check if already used
            Boolean used = (Boolean) redisTemplate.opsForHash().get(key, "used");
            if (Boolean.TRUE.equals(used)) {
                return QrVerificationResult.invalid("QR code already used");
            }

            // Check if nonce is already used (prevent replay)
            String nonceKey = USED_NONCE_PREFIX + payload.getNonce();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nonceKey))) {
                return QrVerificationResult.invalid("QR code already processed");
            }

            // Get vehicle
            Long vehicleId = (Long) redisTemplate.opsForHash().get(key, "vehicleId");
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

            // Verify passcode matches
            if (!vehicle.getFuelPassCode().equals(payload.getPasscode())) {
                return QrVerificationResult.invalid("Passcode mismatch");
            }

            return QrVerificationResult.success(vehicle, payload.getTokenId());

        } catch (Exception e) {
            return QrVerificationResult.invalid("Verification failed: " + e.getMessage());
        }
    }

    // Mark token as used after successful fuel log
    public void markTokenAsUsed(String tokenId, Long staffId) {
        String key = TOKEN_KEY_PREFIX + tokenId;
        redisTemplate.opsForHash().put(key, "used", true);
        redisTemplate.opsForHash().put(key, "usedBy", staffId);
        redisTemplate.opsForHash().put(key, "usedAt", Instant.now().toString());
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
    }

    // Mark nonce as used (prevent replay)
    public void markNonceAsUsed(String nonce) {
        String key = USED_NONCE_PREFIX + nonce;
        redisTemplate.opsForValue().set(key, "1", tokenExpirySeconds, TimeUnit.SECONDS);
    }

    // Invalidate token manually (refresh)
    public void invalidateToken(String tokenId) {
        String key = TOKEN_KEY_PREFIX + tokenId;
        redisTemplate.delete(key);
    }

    private String generateHmacSignature(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(signatureSecret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}