package com.fuelix.controller;

import com.fuelix.config.JwtService;
import com.fuelix.model.Family;
import com.fuelix.model.FamilyMember;
import com.fuelix.model.SharedVehicle;
import com.fuelix.model.SharedWallet;
import com.fuelix.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    @Autowired
    private FamilyService familyService;

    @Autowired
    private JwtService jwtService;

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFamily(@RequestBody Map<String, String> request,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String familyName = request.get("familyName");
            Family family = familyService.createFamily(familyName, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("familyId", family.getId());
            response.put("familyName", family.getFamilyName());
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

            FamilyMember member = familyService.inviteToFamily(familyId, emailOrMobile, userId);

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
            familyService.acceptInvitation(familyId, userId);

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
            familyService.declineInvitation(familyId, userId);

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

            List<Map<String, Object>> invitations = familyService.getPendingInvitations(userId);

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

            familyService.removeFamilyMember(familyId, memberId, userId);

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", familyService.getFamilyInfo(userId)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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

            SharedVehicle shared = familyService.shareVehicle(vehicleId, sharedWithUserId, userId);

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

            familyService.unshareVehicle(vehicleId, sharedWithUserId, userId);

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", familyService.getSharedVehiclesForUser(userId)
            ));
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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", familyService.getVehiclesSharedByOwner(userId)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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

            SharedWallet wallet = familyService.topUpSharedWallet(familyId, userId, amount, method, reference);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "balance", wallet.getBalance(),
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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", familyService.getSharedWalletDetails(familyId, userId)
            ));
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

            boolean canRefuel = familyService.canRefuelSharedVehicle(userId, vehicleId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "canRefuel", canRefuel
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("canRefuel", false));
        }
    }
}