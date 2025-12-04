// src/main/java/com/example/ironplan/model/UserXpEvent.java
package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_xp_events")
@Getter
@Setter
@NoArgsConstructor
public class UserXpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario al que afecta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // +/- XP
    @Column(nullable = false)
    private Integer xpDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private XpEventType type;

    // opcional: descripción humana ("Sesión Tirón 20 series", "Compra rutina Core Pro", etc.)
    @Column(columnDefinition = "TEXT")
    private String description;

    // opcional: si el evento está ligado a una rutina concreta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_template_id")
    private RoutineTemplate routineTemplate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
