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
        name = "workout_sets",
        indexes = {
                @Index(name = "ix_ws_exercise", columnList = "workout_exercise_id, set_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    // --------- INFO DE LA SERIE ---------

    // Serie 1, Serie 2, Serie 3...
    @NotNull
    @Min(1)
    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    // Reps realizadas en esta serie
    @Min(0)
    @Column(name = "reps")
    private Integer reps;

    // Peso usado en kg
    @Min(0)
    @Column(name = "weight_kg")
    private Double weightKg;

    // ¿La serie está marcada como completada? (checkbox verde)
    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    // Notas específicas de esta serie (opcional, por si luego quieres granular)
    @Lob
    @Column(name = "notes")
    private String notes;

    // --------- AYUDA PARA "SERIE ANTERIOR" ---------
    // Puedes usar esto o simplemente leer el último WorkoutSet previo
    @Min(0)
    @Column(name = "previous_reps")
    private Integer previousReps;

    @Min(0)
    @Column(name = "previous_weight_kg")
    private Double previousWeightKg;

    // --------- AUDITORÍA ---------

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
}
