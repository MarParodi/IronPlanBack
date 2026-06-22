package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sus_respuesta",
        uniqueConstraints = @UniqueConstraint(name = "uk_sus_participante", columnNames = {"participante_reto_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SusRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_reto_id", nullable = false)
    private ParticipanteReto participanteReto;

    @Column(name = "fecha_aplicacion", nullable = false, updatable = false)
    private LocalDateTime fechaAplicacion;

    @Column(name = "sus_q1", nullable = false)
    private Integer susQ1;

    @Column(name = "sus_q2", nullable = false)
    private Integer susQ2;

    @Column(name = "sus_q3", nullable = false)
    private Integer susQ3;

    @Column(name = "sus_q4", nullable = false)
    private Integer susQ4;

    @Column(name = "sus_q5", nullable = false)
    private Integer susQ5;

    @Column(name = "sus_q6", nullable = false)
    private Integer susQ6;

    @Column(name = "sus_q7", nullable = false)
    private Integer susQ7;

    @Column(name = "sus_q8", nullable = false)
    private Integer susQ8;

    @Column(name = "sus_q9", nullable = false)
    private Integer susQ9;

    @Column(name = "sus_q10", nullable = false)
    private Integer susQ10;

    @Column(name = "puntaje_sus", precision = 5, scale = 2)
    private BigDecimal puntajeSus;

    @PrePersist
    protected void onCreate() {
        if (fechaAplicacion == null) fechaAplicacion = LocalDateTime.now();
    }
}
