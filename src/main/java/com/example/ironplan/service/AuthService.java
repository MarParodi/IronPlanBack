package com.example.ironplan.service;

import com.example.ironplan.model.User;
import com.example.ironplan.model.Role;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.AuthReq;
import com.example.ironplan.rest.dto.AuthResp;
import com.example.ironplan.rest.dto.RegisterReq;
import com.example.ironplan.security.JwtService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    // Leemos el mismo valor que usa JwtService para calcular expiresAt
    private final long expirationMs;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       @Value("${app.jwt.expirationMs}") long expirationMs) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.expirationMs = expirationMs;
    }

    // -------- REGISTRO --------
    @Transactional
    public AuthResp register(@Valid RegisterReq req) {
        // Normaliza (evita problemas por mayúsculas/minúsculas)
        String email = normalizeEmail(req.getEmail());
        String username = normalizeUsername(req.getUsername());

        // Unicidad
        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }
        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        // Crear usuario
        User user = User.builder()
                .email(email)
                .username(username)
                .password(encoder.encode(req.getPassword()))
                .gender(req.getGender())
                .level(req.getLevel())
                .role(Role.USER)
                .trainDays(req.getTrainDays())
                .birthday(req.getBirthday())
                .build();

        userRepo.save(user);

        // Claims opcionales en el token
        String token = jwt.generateToken(user.getUsername(), Map.of(
                "role", user.getRole().name(),
                "uid", user.getId()
        ));
        long expiresAt = Instant.now().toEpochMilli() + expirationMs;

        return new AuthResp(token, "Bearer", user.getRole().name(), expiresAt);
    }

    // -------- LOGIN (email o username) --------
    public AuthResp login(@Valid AuthReq req) {
        String identifier = req.getIdentifier().trim();
        String password = req.getPassword();

        // ¿Es email?
        User user;
        if (looksLikeEmail(identifier)) {
            String email = normalizeEmail(identifier);
            user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));
        } else {
            String username = normalizeUsername(identifier);
            user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));
        }

        // Verifica contraseña
        if (!encoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }

        String token = jwt.generateToken(user.getUsername(), Map.of(
                "role", user.getRole().name(),
                "uid", user.getId()
        ));
        long expiresAt = Instant.now().toEpochMilli() + expirationMs;

        return new AuthResp(token, "Bearer", user.getRole().name(), expiresAt);
    }

    // -------- Helpers --------
    private static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}
