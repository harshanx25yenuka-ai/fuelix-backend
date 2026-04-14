package com.fuelix.dto;

public class SendOTPRequest {
    private String mobile;
    private String email;

    public SendOTPRequest() {}

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}