// src/main/java/com/example/ironplan/model/WorkoutExercise.java
package com.example.ironplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "workout_exercises",
        indexes = {
                @Index(name = "ix_we_session", columnList = "workout_session_id, exercise_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Sesión real de entrenamiento a la que pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    // De qué ejercicio de plantilla viene (RoutineExercise)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_exercise_id", nullable = true)
    private RoutineExercise routineExercise;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = true)
    private Exercise exercise;


    @Column(name = "exercise_name", nullable = false, length = 150)
    private String exerciseName;

    @Min(1)
    @Column(name = "exercise_order", nullable = false)
    private Integer exerciseOrder = 1;

    @NotNull
    @Min(1)
    @Column(name = "planned_sets", nullable = false)
    private Integer plannedSets;

    @NotNull
    @Min(1)
    @Column(name = "planned_reps_min", nullable = false)
    private Integer plannedRepsMin;

    @NotNull
    @Min(1)
    @Column(name = "planned_reps_max", nullable = false)
    private Integer plannedRepsMax;

    @Min(0)
    @Column(name = "planned_rir")
    private Integer plannedRir;

    @Min(0)
    @Column(name = "planned_rest_seconds")
    private Integer plannedRestSeconds;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutExerciseStatus status = WorkoutExerciseStatus.PENDING;

    @Min(0)
    @Column(name = "completed_sets", nullable = false)
    private Integer completedSets = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;


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

    // --------- HELPERS ---------

    public void markStartedIfNeeded() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
            this.status = WorkoutExerciseStatus.IN_PROGRESS;
        }
    }

    public void markCompleted() {
        this.status = WorkoutExerciseStatus.COMPLETED;
        this.finishedAt = LocalDateTime.now();
    }
}
