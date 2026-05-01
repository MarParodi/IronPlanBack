package com.example.ironplan.model;
 
import jakarta.persistence.*;
import lombok.*;
 
import java.time.LocalDateTime;
 
@Entity
@Table(
    name = "competition_member_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_comp_member",
        columnNames = {"competition_id", "user_id"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CompetitionMemberParticipant {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(nullable = false)
    @Builder.Default
    private Double score = 0.0;
 
    @Column
    private Integer rank;
 
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;
}