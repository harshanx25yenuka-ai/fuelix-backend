package com.fuelix.service;

import com.fuelix.config.JwtService;
import com.fuelix.dto.LoginRequest;
import com.fuelix.dto.LoginResponse;
import com.fuelix.dto.SignupRequest;
import com.fuelix.dto.SignupResponse;
import com.fuelix.model.User;
import com.fuelix.model.Wallet;
import com.fuelix.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FuelLogRepository fuelLogRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TopUpTransactionRepository topUpTransactionRepository;

    @Autowired
    private QuotaRepository quotaRepository;

    @Autowired
    private OTPVerificationRepository otpVerificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OTPService otpService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public SignupResponse register(SignupRequest request) {
        if (userRepository.existsByNic(request.getNic())) {
            throw new RuntimeException("NIC already registered");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        boolean isAdminRegistration = "ADMIN".equals(request.getRole());

        if (!isAdminRegistration) {
            boolean mobileOtpValid = otpService.isOTPVerified(request.getMobile(), "MOBILE");
            if (!mobileOtpValid) {
                throw new RuntimeException("Mobile OTP not verified. Please verify your mobile number first.");
            }

            boolean emailOtpValid = otpService.isOTPVerified(request.getEmail(), "EMAIL");
            if (!emailOtpValid) {
                throw new RuntimeException("Email OTP not verified. Please verify your email address first.");
            }
        }

        User user = new User(
                request.getFirstName(),
                request.getLastName(),
                request.getNic(),
                request.getMobile(),
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getAddressLine3(),
                request.getDistrict(),
                request.getProvince(),
                request.getPostalCode(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(request.getRole() != null ? request.getRole() : "CLIENT");

        User savedUser = userRepository.save(user);

        // Create wallet for the user
        Wallet wallet = new Wallet(savedUser.getId(), 0.0);
        walletRepository.save(wallet);

        // Delete OTP records after successful registration
        otpVerificationRepository.deleteByIdentifierAndType(user.getMobile(), "MOBILE");
        otpVerificationRepository.deleteByIdentifierAndType(user.getEmail(), "EMAIL");

        // Return response with role and mobile
        return new SignupResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getNic(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                savedUser.getRole(),
                "User registered successfully"
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByNic(request.getNic())
                .orElseThrow(() -> new RuntimeException("Invalid NIC or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid NIC or password");
        }

        String token = jwtService.generateToken(user.getNic(), user.getId());

        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("User NIC: " + user.getNic());
        System.out.println("User Role from DB: " + user.getRole());
        System.out.println("User ID: " + user.getId());

        LoginResponse response = new LoginResponse(
                user.getId(),
                token,
                user.getFirstName(),
                user.getLastName(),
                user.getNic(),
                user.getEmail(),
                user.getMobile(),
                user.getAddressLine1() != null ? user.getAddressLine1() : "",
                user.getAddressLine2() != null ? user.getAddressLine2() : "",
                user.getAddressLine3() != null ? user.getAddressLine3() : "",
                user.getDistrict() != null ? user.getDistrict() : "",
                user.getProvince() != null ? user.getProvince() : "",
                user.getPostalCode() != null ? user.getPostalCode() : "",
                user.getRole(), // Make sure role is set
                user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : null
        );

        System.out.println("Login Response Role: " + response.getRole());
        System.out.println("==================");

        return response;
    }

    // Get user by ID
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    // Get user by NIC
    public User getUserByNic(String nic) {
        return userRepository.findByNic(nic)
                .orElseThrow(() -> new RuntimeException("User not found with NIC: " + nic));
    }

    // Get user by Email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean nicExists(String nic) {
        return userRepository.existsByNic(nic);
    }

    public boolean mobileExists(String mobile) {
        return userRepository.existsByMobile(mobile);
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String nic, String reason) {
        User user = userRepository.findByNic(nic)
                .orElseThrow(() -> new RuntimeException("User not found with NIC: " + nic));

        Long userId = user.getId();

        System.out.println("Deleting account for user: " + user.getEmail() + " | Reason: " + reason);

        if (!"ADMIN".equals(user.getRole())) {
            var vehicles = vehicleRepository.findByUserId(userId);
            for (var vehicle : vehicles) {
                var fuelLogs = fuelLogRepository.findByVehicleIdOrderByLoggedAtDesc(vehicle.getId());
                fuelLogRepository.deleteAll(fuelLogs);
                var quotas = quotaRepository.findByVehicleIdOrderByWeekStartDesc(vehicle.getId());
                quotaRepository.deleteAll(quotas);
            }
            vehicleRepository.deleteAll(vehicles);

            var fuelLogs = fuelLogRepository.findByUserIdOrderByLoggedAtDesc(userId);
            fuelLogRepository.deleteAll(fuelLogs);

            var transactions = topUpTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
            topUpTransactionRepository.deleteAll(transactions);

            walletRepository.findByUserId(userId).ifPresent(wallet -> walletRepository.delete(wallet));
        }

        otpVerificationRepository.deleteByIdentifierAndType(user.getEmail(), "EMAIL");
        otpVerificationRepository.deleteByIdentifierAndType(user.getMobile(), "MOBILE");

        userRepository.delete(user);

        System.out.println("Account deleted successfully for user: " + user.getEmail());
    }

    // Update user profile
    @Transactional
    public User updateUserProfile(Long userId, User updatedData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (updatedData.getFirstName() != null) user.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null) user.setLastName(updatedData.getLastName());
        if (updatedData.getMobile() != null) user.setMobile(updatedData.getMobile());
        if (updatedData.getAddressLine1() != null) user.setAddressLine1(updatedData.getAddressLine1());
        if (updatedData.getAddressLine2() != null) user.setAddressLine2(updatedData.getAddressLine2());
        if (updatedData.getAddressLine3() != null) user.setAddressLine3(updatedData.getAddressLine3());
        if (updatedData.getDistrict() != null) user.setDistrict(updatedData.getDistrict());
        if (updatedData.getProvince() != null) user.setProvince(updatedData.getProvince());
        if (updatedData.getPostalCode() != null) user.setPostalCode(updatedData.getPostalCode());
        if (updatedData.getEmail() != null) user.setEmail(updatedData.getEmail());

        return userRepository.save(user);
    }

    // Change password
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}