package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "participante_reto",
        uniqueConstraints = @UniqueConstraint(name = "uk_participante_reto", columnNames = {"reto_id", "usuario_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteReto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reto_id", nullable = false)
    private ExperimentoReto reto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipanteCategoria categoria;

    @Column(name = "objetivo_codigo", nullable = false, length = 10)
    private String objetivoCodigo;

    @Column(name = "objetivo_texto_libre", columnDefinition = "TEXT")
    private String objetivoTextoLibre;

    @Column(name = "fecha_inscripcion", nullable = false, updatable = false)
    private LocalDateTime fechaInscripcion;

    @Column(name = "completo_pretest", nullable = false)
    @Builder.Default
    private Boolean completoPretest = false;

    @Column(name = "completo_posttest", nullable = false)
    @Builder.Default
    private Boolean completoPosttest = false;

    @Column(name = "completo_sus", nullable = false)
    @Builder.Default
    private Boolean completoSus = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        if (fechaInscripcion == null) fechaInscripcion = LocalDateTime.now();
    }
}
