package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "consentimiento_informado",
        uniqueConstraints = @UniqueConstraint(name = "uk_consentimiento", columnNames = {"participante_reto_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentimientoInformado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_reto_id", nullable = false)
    private ParticipanteReto participanteReto;

    @Column(name = "fecha_aceptacion", nullable = false, updatable = false)
    private LocalDateTime fechaAceptacion;

    @Column(name = "ip_dispositivo", length = 45)
    private String ipDispositivo;

    @Column(name = "version_documento", length = 10)
    @Builder.Default
    private String versionDocumento = "1.0";

    @Column(nullable = false)
    private Boolean acepto;

    @PrePersist
    protected void onCreate() {
        if (fechaAceptacion == null) fechaAceptacion = LocalDateTime.now();
    }
}
