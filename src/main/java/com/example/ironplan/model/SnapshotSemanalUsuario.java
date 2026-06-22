package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "snapshot_semanal_usuario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshot_semana",
                columnNames = {"reto_id", "usuario_id", "numero_semana"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotSemanalUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reto_id", nullable = false)
    private ExperimentoReto reto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(name = "numero_semana", nullable = false)
    private Integer numeroSemana;

    @Column(name = "fecha_inicio_semana", nullable = false)
    private LocalDate fechaInicioSemana;

    @Column(name = "fecha_fin_semana", nullable = false)
    private LocalDate fechaFinSemana;

    @Column(name = "sesiones_completadas", nullable = false)
    @Builder.Default
    private Integer sesionesCompletadas = 0;

    @Column(name = "volumen_total_semana", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal volumenTotalSemana = BigDecimal.ZERO;

    @Column(name = "one_rm_promedio", precision = 6, scale = 2)
    private BigDecimal oneRmPromedio;

    @Column(name = "one_rm_maximo", precision = 6, scale = 2)
    private BigDecimal oneRmMaximo;

    @Column(name = "xp_acumulado_al_fin", nullable = false)
    @Builder.Default
    private Integer xpAcumuladoAlFin = 0;

    @Column(name = "xp_ganado_semana", nullable = false)
    @Builder.Default
    private Integer xpGanadoSemana = 0;

    @Column(name = "posicion_leaderboard")
    private Integer posicionLeaderboard;

    @Column(name = "sesiones_cardio", nullable = false)
    @Builder.Default
    private Integer sesionesCardio = 0;

    @Column(name = "minutos_cardio", nullable = false)
    @Builder.Default
    private Integer minutosCardio = 0;

    @Column(name = "km_cardio", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal kmCardio = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
