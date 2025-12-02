// src/main/java/com/example/ironplan/model/RoutineDetail.java
package com.example.ironplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "routine_sessions",
        indexes = {
                @Index(name = "ix_rs_routine", columnList = "routine_id, session_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoutineDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué plantilla de rutina pertenece esta sesión
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private RoutineTemplate routine;

    // Orden dentro de la rutina (Día 1, Día 2, etc.)
    @Min(1)
    @Column(name = "session_order", nullable = false)
    private Integer sessionOrder = 1;

    // Nombre visible: "TIRÓN"
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String title;

    // Icono o emoji: "⚡"
    @Size(max = 16)
    @Column(length = 16)
    private String icon;

    // Músculos principales: "Glúteo, Espalda, Bíceps"
    @Size(max = 255)
    @Column(length = 255)
    private String muscles;

    // Descripción larga de la sesión
    @Lob
    private String description;

    // Total de series de la sesión (para el resumen)
    @Min(0)
    @Column(name = "total_series")
    private Integer totalSeries;

    // Duración estimada en minutos
    @Min(0)
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    // XP estimado que otorga completar esta sesión
    @Min(0)
    @Column(name = "estimated_xp")
    private Integer estimatedXp;

    // ---- Relación con los ejercicios de la sesión ----
    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("exerciseOrder ASC")
    private List<RoutineExercise> exercises = new ArrayList<>();

    // ---- Timestamps ----
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

    // Helpers para mantener la relación bidireccional
    public void addExercise(RoutineExercise ex) {
        exercises.add(ex);
        ex.setSession(this);
    }

    public void removeExercise(RoutineExercise ex) {
        exercises.remove(ex);
        ex.setSession(null);
    }

    // Para agrupar en el bloque
    @Column(nullable = false)
    private Integer blockNumber;   // 1, 2, ...

    @Column(nullable = false)
    private String blockLabel;     // "Semana 1–12"

    @Column(nullable = false)
    private Integer orderInBlock;


}
