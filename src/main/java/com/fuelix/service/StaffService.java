package com.fuelix.service;

import com.fuelix.dto.StaffRequest;
import com.fuelix.dto.StaffResponse;
import com.fuelix.model.FuelStation;
import com.fuelix.model.Staff;
import com.fuelix.model.User;
import com.fuelix.repository.StaffRepository;
import com.fuelix.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FuelStationService fuelStationService;

    @Transactional
    public StaffResponse addStaff(StaffRequest request, Long ownerId) {
        // Check if station exists and belongs to the owner
        var station = fuelStationService.getStationById(request.getStationId());
        if (!station.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("You don't have permission to add staff to this station");
        }

        // Find user by NIC
        User user = userRepository.findByNic(request.getNic())
                .orElseThrow(() -> new RuntimeException("User not found with NIC: " + request.getNic()));

        // Check if user role is CLIENT
        if (!"CLIENT".equals(user.getRole())) {
            throw new RuntimeException("Only users with CLIENT role can be added as staff. Current role: " + user.getRole());
        }

        // Check if user is already a staff member at this station
        if (staffRepository.existsByStaffIdAndStationId(user.getId(), request.getStationId())) {
            throw new RuntimeException("User is already a staff member at this station");
        }

        // Check if user is the station owner
        if (user.getId().equals(ownerId)) {
            throw new RuntimeException("Station owner cannot be added as staff");
        }

        // Create staff record
        Staff staff = new Staff(user.getId(), request.getStationId(), ownerId);
        staffRepository.save(staff);

        // Update user role to STAFF
        user.setRole("STAFF");
        userRepository.save(user);

        // Get added by user info
        User addedByUser = userRepository.findById(ownerId).orElse(null);
        String addedByName = addedByUser != null ? addedByUser.getFirstName() + " " + addedByUser.getLastName() : "Unknown";

        return new StaffResponse(
                staff.getId(),
                user.getId(),
                request.getStationId(),
                user.getFirstName(),
                user.getLastName(),
                user.getNic(),
                user.getEmail(),
                user.getMobile(),
                staff.getCreatedAt(),
                addedByName
        );
    }

    @Transactional
    public void removeStaff(Long staffId, Long stationId, Long ownerId) {
        // Check if station exists and belongs to the owner
        var station = fuelStationService.getStationById(stationId);
        if (!station.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("You don't have permission to remove staff from this station");
        }

        // Find staff record
        Staff staff = staffRepository.findByStaffIdAndStationId(staffId, stationId)
                .orElseThrow(() -> new RuntimeException("Staff member not found at this station"));

        // Get user and update role back to CLIENT
        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete staff record
        staffRepository.delete(staff);

        // Update user role back to CLIENT
        user.setRole("CLIENT");
        userRepository.save(user);
    }

    // Get staff by user ID
    public Staff getStaffByUserId(Long userId) {
        return staffRepository.findByStaffId(userId).orElse(null);
    }

    // Get station by staff ID
    public FuelStation getStationByStaffId(Long staffId, Long stationId) {
        // Verify staff exists at this station
        boolean exists = staffRepository.existsByStaffIdAndStationId(staffId, stationId);
        if (!exists) {
            return null;
        }
        return fuelStationService.getStationById(stationId);
    }

    public List<StaffResponse> getStaffByStation(Long stationId, Long ownerId) {
        // Check if station exists and belongs to the owner
        var station = fuelStationService.getStationById(stationId);
        if (!station.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("You don't have permission to view staff at this station");
        }

        List<Staff> staffList = staffRepository.findByStationId(stationId);

        return staffList.stream().map(staff -> {
            User staffUser = userRepository.findById(staff.getStaffId()).orElse(null);
            User addedByUser = userRepository.findById(staff.getAddedBy()).orElse(null);

            if (staffUser == null) return null;

            return new StaffResponse(
                    staff.getId(),
                    staff.getStaffId(),
                    stationId,
                    staffUser.getFirstName(),
                    staffUser.getLastName(),
                    staffUser.getNic(),
                    staffUser.getEmail(),
                    staffUser.getMobile(),
                    staff.getCreatedAt(),
                    addedByUser != null ? addedByUser.getFirstName() + " " + addedByUser.getLastName() : "Unknown"
            );
        }).filter(response -> response != null).collect(Collectors.toList());
    }

    public long getStaffCount(Long stationId, Long ownerId) {
        var station = fuelStationService.getStationById(stationId);
        if (!station.getOwnerId().equals(ownerId)) {
            return 0;
        }
        return staffRepository.countByStationId(stationId);
    }
}