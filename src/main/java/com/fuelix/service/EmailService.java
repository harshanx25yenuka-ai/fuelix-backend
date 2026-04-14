package com.fuelix.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    @Value("${resend.from.name}")
    private String fromName;

    @Value("${resend.test.mode:true}")
    private boolean testMode;

    @Value("${resend.test.email}")
    private String testEmail;

    public void sendEmail(String toEmail, String subject, String htmlContent) throws Exception {
        // Check if this is your own email address
        boolean isOwnEmail = toEmail.equalsIgnoreCase(testEmail);

        if (testMode && !isOwnEmail) {
            // Test mode for other users - show in console only
            System.out.println("\n========== EMAIL MOCK (Test Mode) ==========");
            System.out.println("From: " + fromName + " <" + fromEmail + ">");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("\n--- Email Content ---");
            // Strip HTML tags for console readability
            String plainText = htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            System.out.println(plainText);
            System.out.println("\n--- OTP Code ---");
            // Extract OTP from HTML
            String otp = extractOTP(htmlContent);
            System.out.println("Your OTP is: " + otp);
            System.out.println("\nNote: In test mode, real emails are only sent to: " + testEmail);
            System.out.println("============================================\n");
            return;
        }

        // For your own email or when test mode is disabled, send real email via Resend
        try {
            Resend resend = new Resend(apiKey);

            SendEmailRequest request = SendEmailRequest.builder()
                    .from(fromName + " <" + fromEmail + ">")
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            SendEmailResponse response = resend.emails().send(request);

            if (response.getId() == null || response.getId().isEmpty()) {
                throw new Exception("Email sending failed: No response ID");
            }

            System.out.println("✅ Email sent successfully to: " + toEmail + " | ID: " + response.getId());

        } catch (ResendException e) {
            System.err.println("❌ Failed to send email to: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            throw new Exception("Email sending failed: " + e.getMessage());
        }
    }

    private String extractOTP(String htmlContent) {
        // Extract 6-digit OTP from HTML content
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{6}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(htmlContent);
        if (matcher.find()) {
            return matcher.group();
        }
        return "Not found";
    }
}