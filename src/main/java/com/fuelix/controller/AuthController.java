package com.fuelix.controller;

import com.fuelix.dto.*;
import com.fuelix.model.User;
import com.fuelix.service.AuthService;
import com.fuelix.service.OTPService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OTPService otpService;

    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Server is running!");
        response.put("message", "Connection successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody SendOTPRequest request) {
        try {
            Map<String, String> response = new HashMap<>();

            if (request.getMobile() != null && !request.getMobile().isEmpty()) {
                String otp = otpService.sendMobileOTP(request.getMobile());
                response.put("message", "OTP sent to mobile");
                response.put("debug_otp", otp);
            } else if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                String otp = otpService.sendEmailOTP(request.getEmail());
                response.put("message", "OTP sent to email");
                response.put("debug_otp", otp);
            } else {
                response.put("error", "Mobile or email is required");
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPRequest request) {
        try {
            boolean isValid = otpService.verifyOTP(
                    request.getIdentifier(),
                    request.getOtp(),
                    request.getType()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            if (isValid) {
                response.put("message", "OTP verified successfully");
            } else {
                response.put("message", "Invalid or expired OTP");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            SignupResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    // NEW ENDPOINT: Get user by ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            User user = authService.getUserById(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("nic", user.getNic());
            response.put("mobile", user.getMobile());
            response.put("addressLine1", user.getAddressLine1() != null ? user.getAddressLine1() : "");
            response.put("addressLine2", user.getAddressLine2() != null ? user.getAddressLine2() : "");
            response.put("addressLine3", user.getAddressLine3() != null ? user.getAddressLine3() : "");
            response.put("district", user.getDistrict() != null ? user.getDistrict() : "");
            response.put("province", user.getProvince() != null ? user.getProvince() : "");
            response.put("postalCode", user.getPostalCode() != null ? user.getPostalCode() : "");
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOTP(@RequestBody ForgotPasswordRequest request) {
        try {
            if (!authService.emailExists(request.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No account found with this email address");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            String otp = otpService.sendEmailOTP(request.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset OTP sent to your email");
            response.put("debug_otp", otp);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOTP(@RequestBody VerifyOTPRequest request) {
        try {
            boolean isValid = otpService.verifyOTP(
                    request.getIdentifier(),
                    request.getOtp(),
                    "EMAIL"
            );

            Map<String, Object> response = new HashMap<>();

            if (isValid) {
                response.put("valid", true);
                response.put("message", "OTP verified. You can now reset your password.");
                String resetToken = otpService.generateResetToken(request.getIdentifier());
                response.put("resetToken", resetToken);
            } else {
                response.put("valid", false);
                response.put("message", "Invalid or expired OTP");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            boolean isValid = otpService.isOTPVerifiedAndValid(
                    request.getEmail(),
                    request.getOtp(),
                    "EMAIL"
            );

            if (!isValid) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid or expired OTP. Please request a new reset code.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            authService.resetPassword(request.getEmail(), request.getNewPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset successfully. Please login with your new password.");
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/admin/register")
    public ResponseEntity<?> adminRegister(@Valid @RequestBody SignupRequest request) {
        try {
            request.setRole("ADMIN");
            SignupResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        try {
            if (!authService.nicExists(request.getNic())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No account found with this NIC number");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            authService.deleteAccount(request.getNic(), request.getReason());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Your account has been permanently deleted.");
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete account. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}