package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "competition_podium_entry",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_podium_rank",
        columnNames = {"competition_id", "scope", "level_category", "rank_position"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionPodiumEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PodiumScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_category", length = 20)
    private ParticipanteCategoria levelCategory;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "composite_score", nullable = false)
    @Builder.Default
    private Double compositeScore = 0.0;

    @Column(name = "consistency_raw", nullable = false)
    @Builder.Default
    private Double consistencyRaw = 0.0;

    @Column(name = "one_rm_progress_raw", nullable = false)
    @Builder.Default
    private Double oneRmProgressRaw = 0.0;

    @Column(name = "volume_raw", nullable = false)
    @Builder.Default
    private Double volumeRaw = 0.0;

    @Column(name = "consistency_norm", nullable = false)
    @Builder.Default
    private Double consistencyNorm = 0.0;

    @Column(name = "one_rm_norm", nullable = false)
    @Builder.Default
    private Double oneRmNorm = 0.0;

    @Column(name = "volume_norm", nullable = false)
    @Builder.Default
    private Double volumeNorm = 0.0;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
