package com.fuelix.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SMSService {

    @Value("${notify.lk.user.id}")
    private String userId;

    @Value("${notify.lk.api.key}")
    private String apiKey;

    @Value("${notify.lk.sender}")
    private String sender;

    @Value("${notify.lk.url}")
    private String apiUrl;

    @Value("${sms.mock.enabled:false}")
    private boolean mockEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendSMS(String mobile, String message) throws Exception {
        // Format mobile number for Notify.lk (must be 11 digits with country code)
        String formattedMobile = formatMobileNumber(mobile);

        if (mockEnabled) {
            System.out.println("========== SMS MOCK ==========");
            System.out.println("To: " + formattedMobile);
            System.out.println("Message: " + message);
            System.out.println("==============================");
            return;
        }

        // Prepare the request body according to Notify.lk API format
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("api_key", apiKey);
        requestBody.put("sender_id", sender);
        requestBody.put("to", formattedMobile);
        requestBody.put("message", message);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Send POST request
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String status = jsonResponse.get("status").asText();

            if (!"success".equals(status)) {
                String errorMsg = jsonResponse.has("errors") ?
                        jsonResponse.get("errors").toString() : "Unknown error";
                throw new Exception("SMS sending failed: " + errorMsg);
            }

            System.out.println("SMS sent successfully to: " + formattedMobile);

        } catch (Exception e) {
            throw new Exception("SMS sending failed: " + e.getMessage());
        }
    }

    /**
     * Format mobile number to Notify.lk requirements:
     * - Must be 11 digits
     * - Must start with country code (94 for Sri Lanka)
     * - Remove any +, spaces, or special characters
     */
    private String formatMobileNumber(String mobile) {
        // Remove all non-digit characters
        String cleaned = mobile.replaceAll("[^0-9]", "");

        // Check if it starts with 94 (Sri Lanka country code)
        if (cleaned.startsWith("94")) {
            // If already has 94, ensure it's exactly 11 digits
            if (cleaned.length() == 11) {
                return cleaned;
            } else if (cleaned.length() > 11) {
                // Take first 11 digits if longer
                return cleaned.substring(0, 11);
            } else if (cleaned.length() < 11) {
                // If less than 11, we need to add digits (shouldn't happen)
                throw new IllegalArgumentException("Invalid mobile number format: " + mobile);
            }
        }
        // If starts with 0 (local format), replace with 94
        else if (cleaned.startsWith("0")) {
            String withoutZero = cleaned.substring(1);
            return "94" + withoutZero;
        }
        // If starts with 7 (missing country code), add 94
        else if (cleaned.startsWith("7") && cleaned.length() == 9) {
            return "94" + cleaned;
        }
        // If starts with 7 and has 10 digits (including 0?), handle
        else if (cleaned.length() == 10 && cleaned.startsWith("7")) {
            return "94" + cleaned;
        }

        // Default: try to format as Sri Lankan number
        if (cleaned.length() == 9 && cleaned.startsWith("7")) {
            return "94" + cleaned;
        } else if (cleaned.length() == 10 && cleaned.startsWith("07")) {
            return "94" + cleaned.substring(1);
        }

        throw new IllegalArgumentException("Unable to format mobile number: " + mobile +
                ". Please provide number in format: 0712345678 or 94712345678");
    }
}