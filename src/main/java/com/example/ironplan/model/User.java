package com.example.ironplan.model;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name= "Users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = true, length = 120)
    private String firstName;

    @Column(nullable = true, length = 120)
    private String lastName;

    @Column(nullable = false)
    private String password;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (xpPoints == null)   xpPoints = 0;
        if (role == null) role = Role.USER;
        if (lifetimeXp == null) lifetimeXp = 0;
        if (xpRank == null)     xpRank = XpRank.NOVATO_I;
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role=Role.USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDate birthday;

    @Builder.Default
    @Column(nullable = false)
    private Integer xpPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Column(nullable = false)
    private Integer trainDays;

    // XP acumulado de por vida (solo suma cuando xpDelta > 0)
    @Builder.Default
    @Column(nullable = false)
    private Integer lifetimeXp = 0;

    // Rango visual basado en lifetimeXp (Novato I, II, etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private XpRank xpRank;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = true)
    private String profilePictureUrl;

    @Column(nullable = true)
    private int weight;

    @Column(nullable = true)
    private int height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 40)
    private Goal goal;

    // ===== Contexto organizacional (fase 1: strings, luego se normaliza a tablas) =====
    @Column(nullable = true, length = 120)
    private String organizationCode;

    @Column(nullable = true, length = 200)
    private String organizationGroup;

    @Column(nullable = true, length = 120)
    private String organizationRole;

    // ===== Consentimientos =====
    @Column(nullable = true)
    private Boolean acceptedTerms;

    @Column(nullable = true)
    private Boolean acceptedPrivacy;

    @Column(nullable = true)
    private Boolean consentProgramMetrics;


    // Rutina actual del usuario (puede ser null si no tiene ninguna activa)
    @ManyToOne
    @JoinColumn(name = "current_routine_id")
    private RoutineTemplate currentRoutine;

    // Fecha en que empezó la rutina actual
    @Column(name = "routine_started_at")
    private LocalDateTime routineStartedAt;

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // Devuelve el username real (no el email que usa Spring Security)
    public String getDisplayUsername() { return username; }
}
