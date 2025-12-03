package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "workout_sessions",
        indexes = {
                @Index(name = "ix_ws_user", columnList = "user_id"),
                @Index(name = "ix_ws_detail", columnList = "routine_detail_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_detail_id", nullable = false)
    private RoutineDetail routineDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutSessionStatus status = WorkoutSessionStatus.ACTIVE;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ------ PROGRESO ------
    @Column(name = "total_exercises", nullable = false)
    private Integer totalExercises = 0; // número total según routineDetail

    @Column(name = "completed_exercises", nullable = false)
    private Integer completedExercises = 0; // cuántos lleva hechos

    @Column(name = "progress_pct", nullable = false)
    private Double progressPercentage = 0.0;

    // ------ XP ------
    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned = 0;

    // ------ TIMESTAMPS ------
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "workoutSession")
    private List<WorkoutExercise> WorkoutExercises;

}
