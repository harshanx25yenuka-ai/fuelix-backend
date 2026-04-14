package com.fuelix.dto;

public class LoginRequest {
    private String nic;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String nic, String password) {
        this.nic = nic;
        this.password = password;
    }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}