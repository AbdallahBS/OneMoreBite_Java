package tn.enicarthage.onemorebite.dto;

public record OtpVerificationRequest(
        String email,
        String code
) {}
