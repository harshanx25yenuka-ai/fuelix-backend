package com.fuelix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelix.model.*;
import com.fuelix.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FamilyService {

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private SharedVehicleRepository sharedVehicleRepository;

    @Autowired
    private SharedWalletRepository sharedWalletRepository;

    @Autowired
    private SharedWalletTransactionRepository sharedWalletTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String OWNER_ROLE = "OWNER";
    private static final String MEMBER_ROLE = "MEMBER";

    private static final String DEFAULT_MEMBER_PERMISSIONS = "{\"can_refuel\":true,\"can_view_quota\":true,\"can_view_wallet\":true,\"can_topup\":true}";
    private static final String DEFAULT_SHARED_VEHICLE_PERMISSIONS = "{\"can_refuel\":true,\"can_view_quota\":true}";

    // ==================== FAMILY MANAGEMENT ====================

    @Transactional
    public Family createFamily(String familyName, Long userId) {
        if (familyRepository.existsByCreatedBy(userId)) {
            throw new RuntimeException("You already have a family");
        }

        Family family = new Family(familyName, userId);
        Family savedFamily = familyRepository.save(family);

        SharedWallet wallet = new SharedWallet(savedFamily.getId(), userId);
        sharedWalletRepository.save(wallet);

        FamilyMember owner = new FamilyMember(savedFamily.getId(), userId, OWNER_ROLE, userId);
        owner.setPermissions("{}");
        owner.setIsActive(true);
        familyMemberRepository.save(owner);

        return savedFamily;
    }

    @Transactional
    public FamilyMember inviteToFamily(Long familyId, String emailOrMobile, Long invitedBy) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Family not found"));

        FamilyMember inviter = familyMemberRepository.findByFamilyIdAndUserId(familyId, invitedBy)
                .orElseThrow(() -> new RuntimeException("Not a family member"));

        if (!OWNER_ROLE.equals(inviter.getRole())) {
            throw new RuntimeException("Only family owner can invite members");
        }

        User user = userRepository.findByEmail(emailOrMobile)
                .orElse(null);

        if (user == null) {
            user = userRepository.findByMobile(emailOrMobile)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        if (familyMemberRepository.existsByFamilyIdAndUserId(familyId, user.getId())) {
            throw new RuntimeException("User already in family");
        }

        if (familyMemberRepository.existsByFamilyIdAndUserIdAndIsActiveFalse(familyId, user.getId())) {
            throw new RuntimeException("Invitation already sent. User needs to accept.");
        }

        FamilyMember member = new FamilyMember(familyId, user.getId(), MEMBER_ROLE, invitedBy);
        member.setPermissions(DEFAULT_MEMBER_PERMISSIONS);
        member.setIsActive(false);
        FamilyMember savedMember = familyMemberRepository.save(member);

        notificationService.createPrivateNotification(
                user.getId(),
                "Family Invitation",
                "You've been invited to join '" + family.getFamilyName() + "' family. Tap to accept or decline.",
                Map.of("type", "FAMILY_INVITE", "familyId", familyId, "familyName", family.getFamilyName())
        );

        return savedMember;
    }

    @Transactional
    public FamilyMember acceptInvitation(Long familyId, Long userId) {
        FamilyMember member = familyMemberRepository.findByFamilyIdAndUserIdAndIsActiveFalse(familyId, userId)
                .orElseThrow(() -> new RuntimeException("No pending invitation found"));

        member.setIsActive(true);
        member.setJoinedAt(LocalDateTime.now());
        FamilyMember savedMember = familyMemberRepository.save(member);

        Family family = familyRepository.findById(familyId).get();
        notificationService.createPrivateNotification(
                family.getCreatedBy(),
                "Member Joined",
                "A new member has joined your family",
                Map.of("type", "MEMBER_JOINED", "userId", userId, "familyId", familyId)
        );

        return savedMember;
    }

    @Transactional
    public void declineInvitation(Long familyId, Long userId) {
        FamilyMember member = familyMemberRepository.findByFamilyIdAndUserIdAndIsActiveFalse(familyId, userId)
                .orElseThrow(() -> new RuntimeException("No pending invitation found"));

        familyMemberRepository.delete(member);
    }

    public List<Map<String, Object>> getPendingInvitations(Long userId) {
        List<FamilyMember> pendingMembers = familyMemberRepository.findByUserIdAndIsActiveFalse(userId);

        List<Map<String, Object>> invitations = new ArrayList<>();

        for (FamilyMember member : pendingMembers) {
            Family family = familyRepository.findById(member.getFamilyId()).orElse(null);
            if (family == null) continue;

            User invitedByUser = userRepository.findById(member.getJoinedBy()).orElse(null);

            Map<String, Object> invitation = new HashMap<>();
            invitation.put("familyId", family.getId());
            invitation.put("familyName", family.getFamilyName());
            invitation.put("invitedBy", invitedByUser != null ? invitedByUser.getFirstName() + " " + invitedByUser.getLastName() : "Unknown");
            invitation.put("invitedAt", member.getJoinedAt());

            invitations.add(invitation);
        }

        return invitations;
    }

    @Transactional
    public void removeFamilyMember(Long familyId, Long memberToRemoveId, Long requestingUserId) {
        FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserId(familyId, requestingUserId)
                .orElseThrow(() -> new RuntimeException("Not a family member"));

        if (!OWNER_ROLE.equals(requester.getRole())) {
            throw new RuntimeException("Only family owner can remove members");
        }

        FamilyMember toRemove = familyMemberRepository.findByFamilyIdAndUserId(familyId, memberToRemoveId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (OWNER_ROLE.equals(toRemove.getRole())) {
            throw new RuntimeException("Cannot remove family owner");
        }

        List<SharedVehicle> sharedVehicles = sharedVehicleRepository.findBySharedWithUserIdAndIsActiveTrue(memberToRemoveId);
        for (SharedVehicle sv : sharedVehicles) {
            sv.setIsActive(false);
            sharedVehicleRepository.save(sv);
        }

        toRemove.setIsActive(false);
        familyMemberRepository.save(toRemove);
    }

    // ==================== SHARED VEHICLE MANAGEMENT ====================

    @Transactional
    public SharedVehicle shareVehicle(Long vehicleId, Long sharedWithUserId, Long ownerUserId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getUserId().equals(ownerUserId)) {
            throw new RuntimeException("You don't own this vehicle");
        }

        FamilyMember ownerMember = familyMemberRepository.findByUserId(ownerUserId).stream()
                .filter(m -> m.getIsActive())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Owner not in any family"));

        FamilyMember sharedMember = familyMemberRepository.findByUserId(sharedWithUserId).stream()
                .filter(m -> m.getFamilyId().equals(ownerMember.getFamilyId()) && m.getIsActive())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not in your family"));

        if (sharedVehicleRepository.existsByVehicleIdAndSharedWithUserId(vehicleId, sharedWithUserId)) {
            throw new RuntimeException("Vehicle already shared with this user");
        }

        SharedVehicle shared = new SharedVehicle(vehicleId, ownerUserId, sharedWithUserId, DEFAULT_SHARED_VEHICLE_PERMISSIONS);
        SharedVehicle saved = sharedVehicleRepository.save(shared);

        User owner = userRepository.findById(ownerUserId).get();
        notificationService.createPrivateNotification(
                sharedWithUserId,
                "Vehicle Shared",
                owner.getFirstName() + " shared " + vehicle.getRegistrationNo() + " with you",
                Map.of("type", "VEHICLE_SHARED", "vehicleId", vehicleId, "vehicleRegNo", vehicle.getRegistrationNo())
        );

        return saved;
    }

    @Transactional
    public void unshareVehicle(Long vehicleId, Long sharedWithUserId, Long ownerUserId) {
        SharedVehicle shared = sharedVehicleRepository.findByVehicleIdAndSharedWithUserId(vehicleId, sharedWithUserId)
                .orElseThrow(() -> new RuntimeException("Vehicle not shared with this user"));

        if (!shared.getOwnerUserId().equals(ownerUserId)) {
            throw new RuntimeException("You don't own this vehicle");
        }

        shared.setIsActive(false);
        sharedVehicleRepository.save(shared);
    }

    public List<Map<String, Object>> getSharedVehiclesForUser(Long userId) {
        List<SharedVehicle> sharedVehicles = sharedVehicleRepository.findBySharedWithUserIdAndIsActiveTrue(userId);

        return sharedVehicles.stream().map(shared -> {
            Vehicle vehicle = vehicleRepository.findById(shared.getVehicleId()).orElse(null);
            if (vehicle == null) return null;

            User owner = userRepository.findById(shared.getOwnerUserId()).orElse(null);

            Map<String, Object> result = new HashMap<>();
            result.put("vehicleId", vehicle.getId());
            result.put("registrationNo", vehicle.getRegistrationNo());
            result.put("make", vehicle.getMake());
            result.put("model", vehicle.getModel());
            result.put("type", vehicle.getType());
            result.put("fuelType", vehicle.getFuelType());
            result.put("ownerId", shared.getOwnerUserId());
            result.put("ownerName", owner != null ? owner.getFirstName() + " " + owner.getLastName() : "Unknown");
            result.put("permissions", parseJsonToMap(shared.getPermissions()));
            result.put("sharedAt", shared.getSharedAt());

            return result;
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getVehiclesSharedByOwner(Long ownerUserId) {
        List<SharedVehicle> sharedVehicles = sharedVehicleRepository.findByOwnerUserIdAndIsActiveTrue(ownerUserId);

        return sharedVehicles.stream().map(shared -> {
            Vehicle vehicle = vehicleRepository.findById(shared.getVehicleId()).orElse(null);
            if (vehicle == null) return null;

            User sharedWith = userRepository.findById(shared.getSharedWithUserId()).orElse(null);

            Map<String, Object> result = new HashMap<>();
            result.put("vehicleId", vehicle.getId());
            result.put("registrationNo", vehicle.getRegistrationNo());
            result.put("sharedWithUserId", shared.getSharedWithUserId());
            result.put("sharedWithName", sharedWith != null ? sharedWith.getFirstName() + " " + sharedWith.getLastName() : "Unknown");
            result.put("sharedAt", shared.getSharedAt());

            return result;
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    // ==================== SHARED WALLET MANAGEMENT ====================

    @Transactional
    public SharedWallet topUpSharedWallet(Long familyId, Long userId, Double amount, String method, String reference) {
        FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .orElseThrow(() -> new RuntimeException("Not a family member"));

        Map<String, Boolean> permissions = parseJsonToMap(member.getPermissions());
        if (!permissions.getOrDefault("can_topup", false) && !OWNER_ROLE.equals(member.getRole())) {
            throw new RuntimeException("You don't have permission to top up");
        }

        SharedWallet wallet = sharedWalletRepository.findByFamilyId(familyId)
                .orElseThrow(() -> new RuntimeException("Shared wallet not found"));

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        SharedWallet updated = sharedWalletRepository.save(wallet);

        SharedWalletTransaction transaction = new SharedWalletTransaction(
                wallet.getId(), userId, amount, "TOPUP", reference
        );
        sharedWalletTransactionRepository.save(transaction);

        List<FamilyMember> members = familyMemberRepository.findByFamilyIdAndIsActiveTrue(familyId);
        User user = userRepository.findById(userId).get();

        for (FamilyMember m : members) {
            if (!m.getUserId().equals(userId)) {
                notificationService.createPrivateNotification(
                        m.getUserId(),
                        "Wallet Top Up",
                        user.getFirstName() + " added LKR " + String.format("%.2f", amount) + " to family wallet",
                        Map.of("type", "WALLET_TOPUP", "amount", amount, "userId", userId)
                );
            }
        }

        return updated;
    }

    @Transactional
    public SharedWallet deductFromSharedWallet(Long familyId, Long userId, Double amount, String reference) {
        FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .orElseThrow(() -> new RuntimeException("Not a family member"));

        Map<String, Boolean> permissions = parseJsonToMap(member.getPermissions());
        if (!permissions.getOrDefault("can_refuel", false) && !OWNER_ROLE.equals(member.getRole())) {
            throw new RuntimeException("You don't have permission to use family wallet");
        }

        SharedWallet wallet = sharedWalletRepository.findByFamilyId(familyId)
                .orElseThrow(() -> new RuntimeException("Shared wallet not found"));

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        SharedWallet updated = sharedWalletRepository.save(wallet);

        SharedWalletTransaction transaction = new SharedWalletTransaction(
                wallet.getId(), userId, amount, "REFUEL", reference
        );
        sharedWalletTransactionRepository.save(transaction);

        return updated;
    }

    public Map<String, Object> getSharedWalletDetails(Long familyId, Long userId) {
        FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .orElseThrow(() -> new RuntimeException("Not a family member"));

        SharedWallet wallet = sharedWalletRepository.findByFamilyId(familyId)
                .orElseThrow(() -> new RuntimeException("Shared wallet not found"));

        List<SharedWalletTransaction> transactions = sharedWalletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        List<Map<String, Object>> transactionList = transactions.stream().map(tx -> {
            User user = userRepository.findById(tx.getUserId()).orElse(null);
            Map<String, Object> txMap = new HashMap<>();
            txMap.put("id", tx.getId());
            txMap.put("amount", tx.getAmount());
            txMap.put("type", tx.getType());
            txMap.put("reference", tx.getReference());
            txMap.put("createdAt", tx.getCreatedAt());
            txMap.put("userName", user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown");
            return txMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("balance", wallet.getBalance());
        result.put("transactions", transactionList);
        result.put("canTopup", permissionsCanTopup(member));
        result.put("canRefuel", permissionsCanRefuel(member));
        result.put("canView", true);

        return result;
    }

    // ==================== QUERIES ====================

    public Map<String, Object> getFamilyInfo(Long userId) {
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(userId);

        if (memberships.isEmpty()) {
            return Map.of("hasFamily", false);
        }

        FamilyMember currentMembership = memberships.stream()
                .filter(m -> m.getIsActive())
                .findFirst()
                .orElse(null);

        if (currentMembership == null) {
            return Map.of("hasFamily", false);
        }

        Family family = familyRepository.findById(currentMembership.getFamilyId()).get();

        List<FamilyMember> allMembers = familyMemberRepository.findByFamilyIdAndIsActiveTrue(family.getId());
        List<Map<String, Object>> memberList = allMembers.stream().map(m -> {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("userId", m.getUserId());
            memberMap.put("name", user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown");
            memberMap.put("email", user != null ? user.getEmail() : "");
            memberMap.put("role", m.getRole());
            memberMap.put("joinedAt", m.getJoinedAt());
            return memberMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("hasFamily", true);
        result.put("familyId", family.getId());
        result.put("familyName", family.getFamilyName());
        result.put("myRole", currentMembership.getRole());
        result.put("members", memberList);
        result.put("myPermissions", parseJsonToMap(currentMembership.getPermissions()));

        return result;
    }

    public boolean canRefuelSharedVehicle(Long userId, Long vehicleId) {
        SharedVehicle shared = sharedVehicleRepository.findByVehicleIdAndSharedWithUserId(vehicleId, userId)
                .orElse(null);

        if (shared == null || !shared.getIsActive()) {
            return false;
        }

        Map<String, Boolean> permissions = parseJsonToMap(shared.getPermissions());
        return permissions.getOrDefault("can_refuel", false);
    }

    // ==================== HELPER METHODS ====================

    private boolean permissionsCanTopup(FamilyMember member) {
        if (OWNER_ROLE.equals(member.getRole())) return true;
        Map<String, Boolean> permissions = parseJsonToMap(member.getPermissions());
        return permissions.getOrDefault("can_topup", false);
    }

    private boolean permissionsCanRefuel(FamilyMember member) {
        if (OWNER_ROLE.equals(member.getRole())) return true;
        Map<String, Boolean> permissions = parseJsonToMap(member.getPermissions());
        return permissions.getOrDefault("can_refuel", false);
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
}