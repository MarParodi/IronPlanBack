package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reto_activity_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reto_activity_review",
                columnNames = {"competition_id", "source", "source_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetoActivityReview {

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
    @Column(name = "source", nullable = false, length = 32)
    private RetoActivitySource source;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "flags", length = 255)
    private String flags;

    @Column(name = "note", length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private RetoActivityReviewStatus status = RetoActivityReviewStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_user_id")
    private User markedBy;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @PrePersist
    void onCreate() {
        if (markedAt == null) markedAt = LocalDateTime.now();
        if (status == null) status = RetoActivityReviewStatus.PENDING_REVIEW;
    }
}
