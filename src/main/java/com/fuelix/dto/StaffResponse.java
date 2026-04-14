package com.fuelix.dto;

import com.fuelix.model.User;
import java.time.LocalDateTime;

public class StaffResponse {
    private Long id;
    private Long staffId;
    private Long stationId;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobile;
    private LocalDateTime addedAt;
    private String addedBy;

    public StaffResponse() {}

    public StaffResponse(Long id, Long staffId, Long stationId, String firstName,
                         String lastName, String nic, String email, String mobile,
                         LocalDateTime addedAt, String addedBy) {
        this.id = id;
        this.staffId = staffId;
        this.stationId = stationId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nic = nic;
        this.email = email;
        this.mobile = mobile;
        this.addedAt = addedAt;
        this.addedBy = addedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
}