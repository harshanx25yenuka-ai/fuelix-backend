package com.fuelix.controller;

import com.fuelix.config.JwtService;
import com.fuelix.model.*;
import com.fuelix.repository.*;
import com.fuelix.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

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
    private JwtService jwtService;

    private static final int MAX_MEMBERS = 4;

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
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

    private String convertMapToJson(Map<String, Boolean> map) {
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (i++ > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        json.append("}");
        return json.toString();
    }

    // ==================== FAMILY MANAGEMENT ====================

    @PostMapping("/create")
    public ResponseEntity<?> createFamily(@RequestBody Map<String, String> request,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String familyName = request.get("familyName");

            if (familyRepository.existsByCreatedBy(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already have a family"));
            }

            Family family = new Family(familyName, userId);
            Family savedFamily = familyRepository.save(family);

            SharedWallet wallet = new SharedWallet(savedFamily.getId(), userId);
            sharedWalletRepository.save(wallet);

            FamilyMember owner = new FamilyMember(savedFamily.getId(), userId, "OWNER", userId);
            owner.setPermissions("{}");
            owner.setIsActive(true);
            familyMemberRepository.save(owner);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("familyId", savedFamily.getId());
            response.put("familyName", savedFamily.getFamilyName());
            response.put("message", "Family created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/invite")
    public ResponseEntity<?> inviteToFamily(@RequestBody Map<String, String> request,
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Long familyId = Long.parseLong(request.get("familyId"));
            String emailOrMobile = request.get("emailOrMobile");

            Family family = familyRepository.findById(familyId)
                    .orElseThrow(() -> new RuntimeException("Family not found"));

            FamilyMember inviter = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("Not a family member"));

            if (!"OWNER".equals(inviter.getRole())) {
                throw new RuntimeException("Only family owner can invite members");
            }

            long activeMemberCount = familyMemberRepository.countByFamilyIdAndIsActiveTrue(familyId);
            if (activeMemberCount >= MAX_MEMBERS) {
                throw new RuntimeException("Family member limit reached (Max " + MAX_MEMBERS + " members)");
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

            FamilyMember member = new FamilyMember(familyId, user.getId(), "MEMBER", userId);

            Map<String, Boolean> defaultPermissions = new HashMap<>();
            defaultPermissions.put("can_refuel", false);
            defaultPermissions.put("can_topup", false);
            defaultPermissions.put("can_view_wallet", true);
            defaultPermissions.put("can_share_vehicle", false);
            member.setPermissions(convertMapToJson(defaultPermissions));
            member.setIsActive(false);
            familyMemberRepository.save(member);

            notificationService.createPrivateNotification(
                    user.getId(),
                    "Family Invitation",
                    "You've been invited to join '" + family.getFamilyName() + "' family. Tap to accept or decline.",
                    Map.of("type", "FAMILY_INVITE", "familyId", familyId, "familyName", family.getFamilyName())
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invitation sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvitation(@RequestBody Map<String, Long> request,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Long familyId = request.get("familyId");

            FamilyMember member = familyMemberRepository.findByFamilyIdAndUserIdAndIsActiveFalse(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("No pending invitation found"));

            member.setIsActive(true);
            member.setJoinedAt(LocalDateTime.now());
            familyMemberRepository.save(member);

            Family family = familyRepository.findById(familyId).get();
            notificationService.createPrivateNotification(
                    family.getCreatedBy(),
                    "Member Joined",
                    "A new member has joined your family",
                    Map.of("type", "MEMBER_JOINED", "userId", userId, "familyId", familyId)
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Joined family successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/decline")
    public ResponseEntity<?> declineInvitation(@RequestBody Map<String, Long> request,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Long familyId = request.get("familyId");

            FamilyMember member = familyMemberRepository.findByFamilyIdAndUserIdAndIsActiveFalse(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("No pending invitation found"));

            familyMemberRepository.delete(member);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invitation declined"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending-invitations")
    public ResponseEntity<?> getPendingInvitations(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", invitations
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/member/{memberId}")
    public ResponseEntity<?> removeFamilyMember(@PathVariable Long memberId,
                                                @RequestParam Long familyId,
                                                @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("Not a family member"));

            if (!"OWNER".equals(requester.getRole())) {
                throw new RuntimeException("Only family owner can remove members");
            }

            FamilyMember toRemove = familyMemberRepository.findByFamilyIdAndUserId(familyId, memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            if ("OWNER".equals(toRemove.getRole())) {
                throw new RuntimeException("Cannot remove family owner");
            }

            List<SharedVehicle> sharedVehicles = sharedVehicleRepository.findBySharedWithUserIdAndIsActiveTrue(memberId);
            for (SharedVehicle sv : sharedVehicles) {
                sv.setIsActive(false);
                sharedVehicleRepository.save(sv);
            }

            toRemove.setIsActive(false);
            familyMemberRepository.save(toRemove);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Member removed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<?> getFamilyInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            List<FamilyMember> memberships = familyMemberRepository.findByUserId(userId);

            if (memberships.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "data", Map.of("hasFamily", false)));
            }

            FamilyMember currentMembership = memberships.stream()
                    .filter(m -> m.getIsActive())
                    .findFirst()
                    .orElse(null);

            if (currentMembership == null) {
                return ResponseEntity.ok(Map.of("success", true, "data", Map.of("hasFamily", false)));
            }

            Family family = familyRepository.findById(currentMembership.getFamilyId()).get();

            List<FamilyMember> allMembers = familyMemberRepository.findByFamilyIdAndIsActiveTrue(family.getId());
            List<Map<String, Object>> memberList = new ArrayList<>();

            for (FamilyMember m : allMembers) {
                User user = userRepository.findById(m.getUserId()).orElse(null);
                if (user == null) continue;

                Map<String, Object> memberMap = new HashMap<>();
                memberMap.put("userId", m.getUserId());
                memberMap.put("name", user.getFirstName() + " " + user.getLastName());
                memberMap.put("email", user.getEmail());
                memberMap.put("role", m.getRole());
                memberMap.put("joinedAt", m.getJoinedAt());
                memberMap.put("permissions", parseJsonToMap(m.getPermissions()));
                memberList.add(memberMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("hasFamily", true);
            result.put("familyId", family.getId());
            result.put("familyName", family.getFamilyName());
            result.put("myRole", currentMembership.getRole());
            result.put("members", memberList);
            result.put("myPermissions", parseJsonToMap(currentMembership.getPermissions()));
            result.put("maxMembers", MAX_MEMBERS);

            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== FAMILY MEMBERS MANAGEMENT ====================

    @GetMapping("/members/{familyId}")
    public ResponseEntity<?> getFamilyMembers(@PathVariable Long familyId,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            List<FamilyMember> members = familyMemberRepository.findByFamilyIdAndIsActiveTrue(familyId);
            List<Map<String, Object>> memberList = new ArrayList<>();

            for (FamilyMember member : members) {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                if (user == null) continue;

                Map<String, Object> memberMap = new HashMap<>();
                memberMap.put("userId", member.getUserId());
                memberMap.put("name", user.getFirstName() + " " + user.getLastName());
                memberMap.put("email", user.getEmail());
                memberMap.put("role", member.getRole());
                memberMap.put("joinedAt", member.getJoinedAt());
                memberMap.put("permissions", parseJsonToMap(member.getPermissions()));
                memberList.add(memberMap);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", memberList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/members/{memberId}/permissions")
    public ResponseEntity<?> updateMemberPermissions(@PathVariable Long memberId,
                                                     @RequestBody Map<String, Object> request,
                                                     @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Long familyId = Long.parseLong(request.get("familyId").toString());
            @SuppressWarnings("unchecked")
            Map<String, Boolean> permissions = (Map<String, Boolean>) request.get("permissions");

            FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("Not a family member"));

            if (!"OWNER".equals(requester.getRole())) {
                throw new RuntimeException("Only family owner can manage permissions");
            }

            FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            String permissionsJson = convertMapToJson(permissions);
            member.setPermissions(permissionsJson);
            familyMemberRepository.save(member);

            return ResponseEntity.ok(Map.of("success", true, "message", "Permissions updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== SHARED VEHICLE MANAGEMENT ====================

    @PostMapping("/share-vehicle")
    public ResponseEntity<?> shareVehicle(@RequestBody Map<String, Object> request,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Long vehicleId = Long.parseLong(request.get("vehicleId").toString());
            Long sharedWithUserId = Long.parseLong(request.get("sharedWithUserId").toString());

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));

            if (!vehicle.getUserId().equals(userId)) {
                throw new RuntimeException("You don't own this vehicle");
            }

            FamilyMember ownerMember = familyMemberRepository.findByUserId(userId).stream()
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

            Map<String, Boolean> defaultPermissions = new HashMap<>();
            defaultPermissions.put("can_refuel", true);
            defaultPermissions.put("can_view_quota", true);
            String permissionsJson = convertMapToJson(defaultPermissions);

            SharedVehicle shared = new SharedVehicle(vehicleId, userId, sharedWithUserId, permissionsJson);
            sharedVehicleRepository.save(shared);

            User owner = userRepository.findById(userId).get();
            notificationService.createPrivateNotification(
                    sharedWithUserId,
                    "Vehicle Shared",
                    owner.getFirstName() + " shared " + vehicle.getRegistrationNo() + " with you",
                    Map.of("type", "VEHICLE_SHARED", "vehicleId", vehicleId, "vehicleRegNo", vehicle.getRegistrationNo())
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vehicle shared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/unshare-vehicle")
    public ResponseEntity<?> unshareVehicle(@RequestParam Long vehicleId,
                                            @RequestParam Long sharedWithUserId,
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            SharedVehicle shared = sharedVehicleRepository.findByVehicleIdAndSharedWithUserId(vehicleId, sharedWithUserId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not shared with this user"));

            if (!shared.getOwnerUserId().equals(userId)) {
                throw new RuntimeException("You don't own this vehicle");
            }

            shared.setIsActive(false);
            sharedVehicleRepository.save(shared);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vehicle unshared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shared-vehicles")
    public ResponseEntity<?> getSharedVehicles(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            List<SharedVehicle> sharedVehicles = sharedVehicleRepository.findBySharedWithUserIdAndIsActiveTrue(userId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (SharedVehicle shared : sharedVehicles) {
                Vehicle vehicle = vehicleRepository.findById(shared.getVehicleId()).orElse(null);
                if (vehicle == null) continue;

                User owner = userRepository.findById(shared.getOwnerUserId()).orElse(null);

                Map<String, Object> vehicleMap = new HashMap<>();
                vehicleMap.put("vehicleId", vehicle.getId());
                vehicleMap.put("registrationNo", vehicle.getRegistrationNo());
                vehicleMap.put("make", vehicle.getMake());
                vehicleMap.put("model", vehicle.getModel());
                vehicleMap.put("type", vehicle.getType());
                vehicleMap.put("fuelType", vehicle.getFuelType());
                vehicleMap.put("ownerId", shared.getOwnerUserId());
                vehicleMap.put("ownerName", owner != null ? owner.getFirstName() + " " + owner.getLastName() : "Unknown");
                vehicleMap.put("permissions", parseJsonToMap(shared.getPermissions()));
                vehicleMap.put("sharedAt", shared.getSharedAt());

                result.add(vehicleMap);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shared-by-me")
    public ResponseEntity<?> getVehiclesSharedByMe(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            List<SharedVehicle> sharedVehicles = sharedVehicleRepository.findByOwnerUserIdAndIsActiveTrue(userId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (SharedVehicle shared : sharedVehicles) {
                Vehicle vehicle = vehicleRepository.findById(shared.getVehicleId()).orElse(null);
                if (vehicle == null) continue;

                User sharedWith = userRepository.findById(shared.getSharedWithUserId()).orElse(null);

                Map<String, Object> vehicleMap = new HashMap<>();
                vehicleMap.put("vehicleId", vehicle.getId());
                vehicleMap.put("registrationNo", vehicle.getRegistrationNo());
                vehicleMap.put("make", vehicle.getMake());
                vehicleMap.put("model", vehicle.getModel());
                vehicleMap.put("type", vehicle.getType());
                vehicleMap.put("fuelType", vehicle.getFuelType());
                vehicleMap.put("ownerId", shared.getOwnerUserId());
                vehicleMap.put("ownerName", "You");
                vehicleMap.put("sharedWithUserId", shared.getSharedWithUserId());
                vehicleMap.put("sharedWithName", sharedWith != null ? sharedWith.getFirstName() + " " + sharedWith.getLastName() : "Unknown");
                vehicleMap.put("sharedAt", shared.getSharedAt());

                Map<String, Boolean> permissions = new HashMap<>();
                permissions.put("can_refuel", true);
                permissions.put("can_view_quota", true);
                vehicleMap.put("permissions", permissions);

                result.add(vehicleMap);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== SHARED WALLET MANAGEMENT ====================

    @PostMapping("/wallet/topup")
    public ResponseEntity<?> topUpSharedWallet(@RequestBody Map<String, Object> request,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Long familyId = Long.parseLong(request.get("familyId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            String method = request.get("method").toString();
            String reference = request.containsKey("reference") ? request.get("reference").toString() : null;

            FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("Not a family member"));

            Map<String, Boolean> permissions = parseJsonToMap(member.getPermissions());
            if (!permissions.getOrDefault("can_topup", false) && !"OWNER".equals(member.getRole())) {
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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "balance", updated.getBalance(),
                    "message", "Wallet topped up successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/wallet/{familyId}")
    public ResponseEntity<?> getSharedWalletDetails(@PathVariable Long familyId,
                                                    @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                    .orElseThrow(() -> new RuntimeException("Not a family member"));

            Map<String, Boolean> memberPermissions = parseJsonToMap(member.getPermissions());

            SharedWallet wallet = sharedWalletRepository.findByFamilyId(familyId)
                    .orElseThrow(() -> new RuntimeException("Shared wallet not found"));

            List<SharedWalletTransaction> transactions = sharedWalletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
            List<Map<String, Object>> transactionList = new ArrayList<>();

            for (SharedWalletTransaction tx : transactions) {
                User user = userRepository.findById(tx.getUserId()).orElse(null);
                Map<String, Object> txMap = new HashMap<>();
                txMap.put("id", tx.getId());
                txMap.put("amount", tx.getAmount());
                txMap.put("type", tx.getType());
                txMap.put("reference", tx.getReference());
                txMap.put("createdAt", tx.getCreatedAt());
                txMap.put("userName", user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown");
                transactionList.add(txMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("balance", wallet.getBalance());
            result.put("transactions", transactionList);
            result.put("canTopup", memberPermissions.getOrDefault("can_topup", false) || "OWNER".equals(member.getRole()));
            result.put("canRefuel", memberPermissions.getOrDefault("can_refuel", false) || "OWNER".equals(member.getRole()));
            result.put("canView", true);

            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/can-refuel/{vehicleId}")
    public ResponseEntity<?> canRefuelSharedVehicle(@PathVariable Long vehicleId,
                                                    @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            SharedVehicle shared = sharedVehicleRepository.findByVehicleIdAndSharedWithUserId(vehicleId, userId)
                    .orElse(null);

            if (shared == null || !shared.getIsActive()) {
                return ResponseEntity.ok(Map.of("canRefuel", false));
            }

            Map<String, Boolean> permissions = parseJsonToMap(shared.getPermissions());
            boolean canRefuel = permissions.getOrDefault("can_refuel", false);

            return ResponseEntity.ok(Map.of("canRefuel", canRefuel));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("canRefuel", false));
        }
    }

    @GetMapping("/can-share-vehicle")
    public ResponseEntity<?> canShareVehicle(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            List<FamilyMember> memberships = familyMemberRepository.findByUserId(userId);
            if (memberships.isEmpty()) {
                return ResponseEntity.ok(Map.of("canShareVehicle", false));
            }

            FamilyMember currentMembership = memberships.stream()
                    .filter(m -> m.getIsActive())
                    .findFirst()
                    .orElse(null);

            if (currentMembership == null) {
                return ResponseEntity.ok(Map.of("canShareVehicle", false));
            }

            if ("OWNER".equals(currentMembership.getRole())) {
                return ResponseEntity.ok(Map.of("canShareVehicle", true));
            }

            Map<String, Boolean> permissions = parseJsonToMap(currentMembership.getPermissions());
            boolean canShareVehicle = permissions.getOrDefault("can_share_vehicle", false);

            return ResponseEntity.ok(Map.of("canShareVehicle", canShareVehicle));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("canShareVehicle", false));
        }
    }
}