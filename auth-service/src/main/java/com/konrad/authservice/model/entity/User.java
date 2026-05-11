package com.konrad.authservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String passwordHash;   // RNF: contraseña almacenada encriptada
    private Role role;             // ADMIN | DIRECTOR | SELLER | BUYER
    private String relatedEntityId; // sellerId o buyerId según el rol
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public enum Role {
        ADMIN, DIRECTOR, SELLER, BUYER
    }
}
