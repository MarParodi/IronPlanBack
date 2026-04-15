package com.example.ironplan.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "OnboardingSessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingSession {

    @Id
    @Column(length = 36)
    private String token; // UUID

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * 1..4 (último paso completado)
     */
    @Column(nullable = false)
    private Integer completedStep;

    // ===== Paso 1: Registro básico =====
    @Column(nullable = false, length = 120)
    private String firstName;

    @Column(nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, length = 120)
    private String username;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    // ===== Paso 2: Perfil inicial =====
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer weight;

    private Integer height;

    @Enumerated(EnumType.STRING)
    private Level level;

    private Integer trainDays;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    // ===== Paso 3: Contexto organizacional =====
    @Column(nullable = true, length = 120)
    private String organizationCode;

    @Column(nullable = true, length = 200)
    private String organizationGroup;

    @Column(nullable = true, length = 120)
    private String organizationRole;

    // ===== Paso 4: Consentimientos =====
    private Boolean acceptedTerms;
    private Boolean acceptedPrivacy;
    private Boolean consentProgramMetrics;
}

