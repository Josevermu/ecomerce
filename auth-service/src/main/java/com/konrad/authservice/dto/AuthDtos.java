package com.konrad.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String role;
        private String userId;
        private String relatedEntityId;
        private long expiresInMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        private String email;
        // RNF Seguridad: mínimo 8 chars, mayúscula, minúscula y número
        // Validado en AuthService.validatePasswordPolicy()
        private String password;
        private String role;        // SELLER | BUYER
        private String relatedEntityId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private String id;
        private String email;
        private String role;
        private boolean active;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
}
