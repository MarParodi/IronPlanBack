package com.example.ironplan.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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

/**
 * Representa un bloque o fase dentro de una rutina de entrenamiento.
 * Ejemplo: Mesociclo 1, Fase de Fuerza, Semanas 1-4, etc.
 * 
 * Jerarquía: RoutineTemplate (1) → (N) RoutineBlock (1) → (N) RoutineDetail
 */
@Entity
@Table(
        name = "routine_blocks",
        indexes = {
                @Index(name = "ix_rb_routine", columnList = "routine_id, order_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoutineBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué plantilla de rutina pertenece este bloque
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    @JsonBackReference
    private RoutineTemplate routine;

    // Nombre del bloque: "Mesociclo 1", "Fase de Fuerza", etc.
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    // Descripción del bloque
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    // Orden del bloque dentro de la rutina (1, 2, 3...)
    @Min(1)
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    // Duración del bloque en semanas
    @Min(1)
    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks = 1;

    // ---- Relación con las sesiones del bloque ----
    @OneToMany(
            mappedBy = "block",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sessionOrder ASC")
    @JsonManagedReference(value = "block-sessions")
    private List<RoutineDetail> sessions = new ArrayList<>();

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
    public void addSession(RoutineDetail session) {
        sessions.add(session);
        session.setBlock(this);
    }

    public void removeSession(RoutineDetail session) {
        sessions.remove(session);
        session.setBlock(null);
    }
}

