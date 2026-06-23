package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "competition_declared_winner",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_declared_winner",
        columnNames = {"competition_id", "scope", "level_category"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionDeclaredWinner {

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

    @Column(name = "declared_at", nullable = false)
    private LocalDateTime declaredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declared_by_user_id", nullable = false)
    private User declaredBy;
}
