package com.konrad.authservice.service;

import com.konrad.authservice.config.JwtUtil;
import com.konrad.authservice.dto.AuthDtos.*;
import com.konrad.authservice.model.entity.User;
import com.konrad.authservice.model.entity.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // RNF Seguridad: mínimo 8 chars, una mayúscula, una minúscula, un número
    private static final Pattern PASSWORD_POLICY =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new RuntimeException("Usuario inactivo");
        }

        // Mock: comparación directa. En producción: BCrypt.matches()
        if (!mockPasswordMatch(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        log.info("[AUTH] Login exitoso: {} ({})", user.getEmail(), user.getRole());

        return LoginResponse.builder()
            .token(token)
            .role(user.getRole().name())
            .userId(user.getId())
            .relatedEntityId(user.getRelatedEntityId())
            .expiresInMs(jwtUtil.getExpirationMs())
            .build();
    }

    // -------------------------------------------------------------------------
    // Registro de nuevo usuario (llamado desde seller-service y buyer-service
    // cuando una solicitud es aprobada)
    // -------------------------------------------------------------------------
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email ya registrado: " + request.getEmail());
        }

        validatePasswordPolicy(request.getPassword());

        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(mockHash(request.getPassword())) // En prod: BCrypt.encode()
            .role(User.Role.valueOf(request.getRole()))
            .relatedEntityId(request.getRelatedEntityId())
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

        User saved = userRepository.save(user);
        log.info("[AUTH] Usuario registrado: {} con rol {}", saved.getEmail(), saved.getRole());

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Cambio de contraseña
    // -------------------------------------------------------------------------
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!mockPasswordMatch(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        validatePasswordPolicy(request.getNewPassword());
        user.setPasswordHash(mockHash(request.getNewPassword()));
        userRepository.save(user);
        log.info("[AUTH] Contraseña cambiada para usuario: {}", userId);
    }

    // -------------------------------------------------------------------------
    // Validación de token (endpoint para que el gateway valide)
    // -------------------------------------------------------------------------
    public boolean validateToken(String token) {
        return jwtUtil.isValid(token);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    /** RNF Seguridad: política de contraseña */
    private void validatePasswordPolicy(String password) {
        if (!PASSWORD_POLICY.matcher(password).matches()) {
            throw new RuntimeException(
                "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número");
        }
    }

    /** Mock hash: en producción BCryptPasswordEncoder */
    private String mockHash(String plain) { return "$MOCK$" + plain; }
    private boolean mockPasswordMatch(String plain, String hash) {
        return hash.equals("$MOCK$" + plain);
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
            .id(u.getId()).email(u.getEmail())
            .role(u.getRole().name()).active(u.isActive())
            .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
            .build();
    }
}
