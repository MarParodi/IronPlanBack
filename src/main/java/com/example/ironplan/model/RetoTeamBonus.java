package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reto_team_bonus",
        indexes = @Index(name = "ix_rtb_competition_group", columnList = "competition_id, organizational_group_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetoTeamBonus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizational_group_id", nullable = false)
    private OrganizationalGroup group;

    @Column(name = "points", nullable = false)
    private Double points;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "evidence_public_id", length = 255)
    private String evidencePublicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
