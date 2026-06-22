package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ipaq_respuesta",
        uniqueConstraints = @UniqueConstraint(name = "uk_ipaq_corte", columnNames = {"participante_reto_id", "corte"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpaqRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_reto_id", nullable = false)
    private ParticipanteReto participanteReto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IpaqCorte corte;

    @Column(name = "fecha_aplicacion", nullable = false, updatable = false)
    private LocalDateTime fechaAplicacion;

    @Column(name = "caminata_dias_semana")
    private Integer caminataDiasSemana;

    @Column(name = "caminata_min_dia")
    private Integer caminataMinDia;

    @Column(name = "moderada_dias_semana")
    private Integer moderadaDiasSemana;

    @Column(name = "moderada_min_dia")
    private Integer moderadaMinDia;

    @Column(name = "vigorosa_dias_semana")
    private Integer vigorosaDiasSemana;

    @Column(name = "vigorosa_min_dia")
    private Integer vigorosaMinDia;

    @Column(name = "met_caminata", precision = 10, scale = 2)
    private BigDecimal metCaminata;

    @Column(name = "met_moderada", precision = 10, scale = 2)
    private BigDecimal metModerada;

    @Column(name = "met_vigorosa", precision = 10, scale = 2)
    private BigDecimal metVigorosa;

    @Column(name = "met_total_semana", precision = 10, scale = 2)
    private BigDecimal metTotalSemana;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_ipaq", length = 20)
    private CategoriaIpaq categoriaIpaq;

    @Column(name = "es_outlier", nullable = false)
    @Builder.Default
    private Boolean esOutlier = false;

    @PrePersist
    protected void onCreate() {
        if (fechaAplicacion == null) fechaAplicacion = LocalDateTime.now();
    }
}
