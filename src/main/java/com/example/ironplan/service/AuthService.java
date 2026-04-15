package com.example.ironplan.service;

import com.example.ironplan.model.User;
import com.example.ironplan.model.OnboardingSession;
import com.example.ironplan.model.Role;
import com.example.ironplan.repository.OnboardingSessionRepository;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.AuthReq;
import com.example.ironplan.rest.dto.AuthResp;
import com.example.ironplan.rest.dto.RegisterReq;
import com.example.ironplan.rest.dto.RegisterStep1Req;
import com.example.ironplan.rest.dto.RegisterStep1Resp;
import com.example.ironplan.rest.dto.RegisterStep2Req;
import com.example.ironplan.rest.dto.RegisterStep3Req;
import com.example.ironplan.rest.dto.RegisterStep4Req;
import com.example.ironplan.security.JwtService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final OnboardingSessionRepository onboardingRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    // Leemos el mismo valor que usa JwtService para calcular expiresAt
    private final long expirationMs;

    public AuthService(UserRepository userRepo,
                       OnboardingSessionRepository onboardingRepo,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       @Value("${app.jwt.expirationMs}") long expirationMs) {
        this.userRepo = userRepo;
        this.onboardingRepo = onboardingRepo;
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

    // -------- ONBOARDING: PASO 1 --------
    @Transactional
    public RegisterStep1Resp registerStep1(@Valid RegisterStep1Req req) {
        if (req.getPassword() == null || !req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        String email = normalizeEmail(req.getEmail());
        String username = normalizeUsername(req.getUsername());

        if (userRepo.existsByEmail(email) || onboardingRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }
        if (userRepo.existsByUsername(username) || onboardingRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS);

        OnboardingSession session = OnboardingSession.builder()
                .token(token)
                .createdAt(now)
                .expiresAt(expiresAt)
                .completedStep(1)
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .email(email)
                .username(username)
                .passwordHash(encoder.encode(req.getPassword()))
                .build();

        onboardingRepo.save(session);
        return new RegisterStep1Resp(token, expiresAt.toEpochMilli());
    }

    // -------- ONBOARDING: PASO 2 --------
    @Transactional
    public void registerStep2(@Valid RegisterStep2Req req) {
        OnboardingSession session = requireValidSession(req.getOnboardingToken());
        requireStepOrder(session, 1, 2);

        session.setBirthday(req.getBirthday());
        session.setGender(req.getGender());
        session.setLevel(req.getLevel());
        session.setTrainDays(req.getTrainDays());
        session.setWeight(req.getWeight());
        session.setHeight(req.getHeight());
        session.setGoal(req.getGoal());
        session.setCompletedStep(2);

        onboardingRepo.save(session);
    }

    // -------- ONBOARDING: PASO 3 --------
    @Transactional
    public void registerStep3(@Valid RegisterStep3Req req) {
        OnboardingSession session = requireValidSession(req.getOnboardingToken());
        requireStepOrder(session, 2, 3);

        String code = req.getOrganizationCode() != null ? req.getOrganizationCode().trim() : null;
        String group = req.getOrganizationGroup() != null ? req.getOrganizationGroup().trim() : null;
        String role = req.getOrganizationRole() != null ? req.getOrganizationRole().trim() : null;

        if (code != null && !code.isEmpty()) {
            session.setOrganizationCode(code);
        }
        if (group != null && !group.isEmpty()) {
            session.setOrganizationGroup(group);
        }
        if (role != null && !role.isEmpty()) {
            session.setOrganizationRole(role);
        }

        // Marcamos que pasó por el paso 3 (aunque sea sin datos)
        session.setCompletedStep(3);

        onboardingRepo.save(session);
    }

    // -------- ONBOARDING: PASO 4 (FINALIZA) --------
    @Transactional
    public AuthResp registerStep4(@Valid RegisterStep4Req req) {
        OnboardingSession session = requireValidSession(req.getOnboardingToken());
        // Debe haber completado al menos el paso 2 (perfil inicial).
        Integer completed = session.getCompletedStep() != null ? session.getCompletedStep() : 0;
        if (completed < 2) {
            throw new IllegalArgumentException("Debes completar el paso 2 antes de finalizar el registro.");
        }

        session.setAcceptedTerms(req.isAcceptedTerms());
        session.setAcceptedPrivacy(req.isAcceptedPrivacy());
        session.setConsentProgramMetrics(req.isConsentProgramMetrics());
        session.setCompletedStep(4);
        onboardingRepo.save(session);

        // Re-check unicidad al momento de crear usuario (por carreras)
        if (userRepo.existsByEmail(session.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }
        if (userRepo.existsByUsername(session.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        User user = User.builder()
                .email(session.getEmail())
                .username(session.getUsername())
                .firstName(session.getFirstName())
                .lastName(session.getLastName())
                .password(session.getPasswordHash())
                .gender(session.getGender())
                .level(session.getLevel())
                .role(Role.USER)
                .trainDays(session.getTrainDays())
                .birthday(session.getBirthday())
                .goal(session.getGoal())
                .organizationCode(session.getOrganizationCode())
                .organizationGroup(session.getOrganizationGroup())
                .organizationRole(session.getOrganizationRole())
                .acceptedTerms(Boolean.TRUE.equals(session.getAcceptedTerms()))
                .acceptedPrivacy(Boolean.TRUE.equals(session.getAcceptedPrivacy()))
                .consentProgramMetrics(Boolean.TRUE.equals(session.getConsentProgramMetrics()))
                .weight(session.getWeight() != null ? session.getWeight() : 0)
                .height(session.getHeight() != null ? session.getHeight() : 0)
                .build();

        userRepo.save(user);
        onboardingRepo.deleteById(session.getToken());

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

    private OnboardingSession requireValidSession(String token) {
        OnboardingSession session = onboardingRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado o expirado."));
        if (session.getExpiresAt() == null || session.getExpiresAt().isBefore(Instant.now())) {
            onboardingRepo.deleteById(session.getToken());
            throw new IllegalArgumentException("Registro expirado. Vuelve a iniciar el registro.");
        }
        return session;
    }

    private static void requireStepOrder(OnboardingSession session, int requiredCompletedStep, int stepToComplete) {
        Integer completed = session.getCompletedStep() != null ? session.getCompletedStep() : 0;
        if (completed < requiredCompletedStep) {
            throw new IllegalArgumentException("Debes completar el paso " + requiredCompletedStep + " antes de continuar.");
        }
        if (completed >= stepToComplete) {
            // idempotencia básica: permitir reintentos del mismo paso o pasos anteriores sin romper
            return;
        }
    }
}
