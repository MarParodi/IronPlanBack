package com.example.ironplan.model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Routine_Template")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoutineTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String longDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal goal;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User user;

    @Column(nullable = false)
    private Boolean isPublic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Access_Type access;

    private int xp_cost;

    @Column(nullable = false)
    private int days_per_week;

    @Column(nullable = false)
    private int xp_gain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineStatus status = RoutineStatus.DRAFT;

    @PrePersist
    public void OnCreate() {
        LocalDateTime now = LocalDateTime.now();
    }

    @Column(nullable = false,  updatable = false)
    private LocalDateTime CreatedAt;

    // en RoutineTemplate.java
    @Column(name = "img", length = 255)
    private String img;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestedLevel", nullable = false)
    private Level suggestedLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutineGender routineGender = RoutineGender.UNISEX;

    @Column(name = "durationWeeks")
    private int durationWeeks;

    // Contador de veces que se ha usado esta rutina
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    // REFACTORIZADO: Ahora RoutineTemplate tiene una lista de BLOCKS, no de sessions directamente
    // La jerarquía es: RoutineTemplate (1) → (N) RoutineBlock (1) → (N) RoutineDetail
    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @JsonManagedReference
    private List<RoutineBlock> blocks = new ArrayList<>();

    // Helpers para mantener la relación bidireccional
    public void addBlock(RoutineBlock block) {
        blocks.add(block);
        block.setRoutine(this);
    }

    public void removeBlock(RoutineBlock block) {
        blocks.remove(block);
        block.setRoutine(null);
    }
}
