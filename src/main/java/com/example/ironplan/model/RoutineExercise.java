// src/main/java/com/example/ironplan/model/RoutineExercise.java
package com.example.ironplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "routine_exercises",
        indexes = {
                @Index(name = "ix_re_session", columnList = "session_id, exercise_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoutineExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué sesión pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RoutineDetail session;

    // Relación con el catálogo de ejercicios (peso muerto, remo, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    // Para no depender solo del nombre del catálogo, puedes guardar un "alias"
    @Column(name = "display_name", length = 150)
    private String displayName;

    // Orden de aparición dentro de la sesión (1 = primero)
    @Min(1)
    @Column(name = "exercise_order", nullable = false)
    private Integer exerciseOrder = 1;

    // Nº de series
    @NotNull
    @Min(1)
    @Column(name = "sets", nullable = false)
    private Integer sets;

    // Rango de reps: 7-9, 8-10, etc.
    @NotNull
    @Min(1)
    @Column(name = "reps_min", nullable = false)
    private Integer repsMin;

    @NotNull
    @Min(1)
    @Column(name = "reps_max", nullable = false)
    private Integer repsMax;

    // RIR objetivo: 1, 2, etc.
    @Min(0)
    @Column(name = "rir")
    private Integer rir;

    // Descanso opcional en segundos
    @Min(0)
    @Column(name = "rest_min")
    private Integer restMinutes;
}
