package com.fuelix.service;

import com.fuelix.model.OTPVerification;
import com.fuelix.repository.OTPVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class OTPService {

    @Autowired
    private OTPVerificationRepository otpRepository;

    @Autowired
    private SMSService smsService;

    @Autowired
    private EmailService emailService;

    @Value("${otp.expiry.minutes}")
    private int expiryMinutes;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${resend.test.email}")
    private String testEmail;

    // Store temporary reset tokens (in production, use Redis with expiry)
    private final Map<String, ResetToken> resetTokens = new HashMap<>();

    private static class ResetToken {
        String email;
        LocalDateTime createdAt;

        ResetToken(String email) {
            this.email = email;
            this.createdAt = LocalDateTime.now();
        }

        boolean isValid() {
            return createdAt.plusMinutes(10).isAfter(LocalDateTime.now());
        }
    }

    private String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Transactional
    public String sendMobileOTP(String mobile) throws Exception {
        String cleanMobile = mobile.replaceAll("[^0-9]", "");
        if (cleanMobile.startsWith("94")) {
            cleanMobile = cleanMobile.substring(2);
        }
        if (cleanMobile.startsWith("0")) {
            cleanMobile = cleanMobile.substring(1);
        }

        String otp = generateOTP();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        // Delete old unverified OTP only
        OTPVerification existing = otpRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(mobile, "MOBILE")
                .orElse(null);

        if (existing != null && !existing.isVerified()) {
            otpRepository.deleteByIdentifierAndType(mobile, "MOBILE");
        }

        OTPVerification otpVerification = new OTPVerification(mobile, otp, "MOBILE", expiresAt);
        otpRepository.save(otpVerification);

        String message = "Your Fuelix verification code is: " + otp + ". Valid for " + expiryMinutes + " minutes.";
        smsService.sendSMS(cleanMobile, message);

        System.out.println("\n📱 Mobile OTP for " + mobile + ": " + otp);

        return otp;
    }

    @Transactional
    public String sendEmailOTP(String email) throws Exception {
        String otp = generateOTP();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        // Delete old unverified OTP only
        OTPVerification existing = otpRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(email, "EMAIL")
                .orElse(null);

        if (existing != null && !existing.isVerified()) {
            otpRepository.deleteByIdentifierAndType(email, "EMAIL");
        }

        OTPVerification otpVerification = new OTPVerification(email, otp, "EMAIL", expiresAt);
        otpRepository.save(otpVerification);

        String subject = "Fuelix - Password Reset Verification";

        boolean isOwnEmail = email.equalsIgnoreCase(testEmail);

        String content = "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px; }" +
                ".container { max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".header { text-align: center; margin-bottom: 20px; }" +
                ".logo { display: inline-block; background: linear-gradient(135deg, #00C896, #0A84FF); padding: 12px 24px; border-radius: 8px; }" +
                ".logo-text { color: white; font-size: 20px; font-weight: bold; }" +
                "h2 { color: #00C896; text-align: center; }" +
                ".otp-box { text-align: center; margin: 20px 0; }" +
                ".otp-code { font-size: 36px; font-weight: bold; color: #0A84FF; letter-spacing: 4px; background-color: #f0f0f0; padding: 12px 24px; border-radius: 8px; display: inline-block; }" +
                ".info { color: #666; text-align: center; }" +
                ".warning { background-color: #FFF3E0; border-left: 4px solid #FF9800; padding: 12px; margin-top: 20px; }" +
                ".footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; color: #999; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<div class='logo'><span class='logo-text'>FUELIX</span></div>" +
                "</div>" +
                "<h2>Password Reset Request</h2>" +
                "<p class='info'>We received a request to reset your password. Use the verification code below:</p>" +
                "<div class='otp-box'>" +
                "<div class='otp-code'>" + otp + "</div>" +
                "</div>" +
                "<p class='info'>This code will expire in " + expiryMinutes + " minutes.</p>" +
                "<div class='warning'>" +
                "<p style='margin: 0; font-size: 13px;'>⚠️ If you didn't request this, please ignore this email. Your password will not be changed.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Fuelix - Smart Fuel Management</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        System.out.println("\n✉️  Password Reset OTP for " + email + ": " + otp);
        if (!isOwnEmail) {
            System.out.println("   (This email will be shown in console only - real email sent to: " + testEmail + ")");
        }

        emailService.sendEmail(email, subject, content);

        return otp;
    }

    @Transactional
    public boolean verifyOTP(String identifier, String otp, String type) {
        OTPVerification otpRecord = otpRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, type)
                .orElse(null);

        if (otpRecord == null) {
            System.out.println("OTP verification failed: No record found for " + identifier);
            return false;
        }

        if (otpRecord.isVerified()) {
            System.out.println("OTP verification failed: Already verified for " + identifier);
            return false;
        }

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            System.out.println("OTP verification failed: Expired for " + identifier);
            return false;
        }

        if (!otpRecord.getOtpCode().equals(otp)) {
            System.out.println("OTP verification failed: Code mismatch for " + identifier + ". Expected: " + otpRecord.getOtpCode() + ", Got: " + otp);
            return false;
        }

        // Mark as verified but DON'T delete - keep for password reset
        otpRecord.setVerified(true);
        otpRepository.save(otpRecord);

        System.out.println("OTP verified successfully for " + identifier);

        return true;
    }

    // Special method for password reset that doesn't require re-verification
    @Transactional(readOnly = true)
    public boolean isOTPVerifiedAndValid(String identifier, String otp, String type) {
        OTPVerification otpRecord = otpRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, type)
                .orElse(null);

        if (otpRecord == null) {
            System.out.println("OTP check failed: No record found for " + identifier);
            return false;
        }

        if (!otpRecord.isVerified()) {
            System.out.println("OTP check failed: Not verified for " + identifier);
            return false;
        }

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            System.out.println("OTP check failed: Expired for " + identifier);
            return false;
        }

        if (!otpRecord.getOtpCode().equals(otp)) {
            System.out.println("OTP check failed: Code mismatch for " + identifier);
            return false;
        }

        System.out.println("OTP is valid and verified for " + identifier);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isOTPVerified(String identifier, String type) {
        OTPVerification otpRecord = otpRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, type)
                .orElse(null);

        if (otpRecord == null) {
            return false;
        }

        return otpRecord.isVerified() && otpRecord.getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void deleteOTP(String identifier, String type) {
        // Optional cleanup
    }

    public String generateResetToken(String email) {
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new ResetToken(email));

        // Clean up old tokens
        resetTokens.entrySet().removeIf(entry -> !entry.getValue().isValid());

        return token;
    }

    public boolean validateResetToken(String token, String email) {
        ResetToken resetToken = resetTokens.get(token);
        if (resetToken == null || !resetToken.isValid()) {
            return false;
        }
        return resetToken.email.equals(email);
    }
}