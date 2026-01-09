package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Definición de hazañas/logros disponibles en el sistema.
 * Cada hazaña tiene un código único, nombre, descripción, icono y XP que otorga.
 */
@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // ej: FIRST_WORKOUT, TEN_WORKOUTS, etc.

    @Column(nullable = false, length = 100)
    private String name; // ej: "Primera Rutina"

    @Column(nullable = false, length = 500)
    private String description; // ej: "Completa tu primer entrenamiento"

    @Column(nullable = false, length = 50)
    private String icon; // ej: "trophy", "medal", "fire", "star"

    @Column(nullable = false, length = 20)
    private String category; // ej: "workout", "xp", "social", "creator"

    @Column(nullable = false)
    private Integer xpReward = 0; // XP que se otorga al desbloquear

    @Column(nullable = false)
    private Integer sortOrder = 0; // Orden de visualización

    @Column(nullable = false)
    private Boolean isActive = true; // Si está activa o no

    // Constructor para facilitar la creación
    public Achievement(String code, String name, String description, String icon, String category, Integer xpReward, Integer sortOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.xpReward = xpReward;
        this.sortOrder = sortOrder;
        this.isActive = true;
    }
}
