package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "competition_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_competition_group",
        columnNames = {"competition_id", "organizational_group_id"}
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionParticipant {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizational_group_id", nullable = false)
    private OrganizationalGroup group;
 
    @Column(name = "group_score", nullable = false)
    @Builder.Default
    private Double groupScore = 0.0;
 
    @Column
    private Integer rank; // Posición en el ranking grupal
 
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

}
