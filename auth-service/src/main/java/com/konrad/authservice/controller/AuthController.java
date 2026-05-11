package com.konrad.authservice.controller;

import com.konrad.authservice.dto.AuthDtos;
import com.konrad.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /auth/login — obtener token JWT */
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.LoginResponse> login(@RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** POST /auth/register — registrar usuario (llamado internamente desde otros servicios) */
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.UserResponse> register(@RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /** POST /auth/validate — el gateway llama este endpoint para validar tokens */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /** POST /auth/change-password */
    @PostMapping("/change-password/{userId}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable String userId,
            @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    /** GET /auth/users — solo ADMIN */
    @GetMapping("/users")
    public ResponseEntity<List<AuthDtos.UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.findAll());
    }
}
