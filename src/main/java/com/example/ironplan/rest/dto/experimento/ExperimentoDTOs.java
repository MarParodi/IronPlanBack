package com.example.ironplan.rest.dto.experimento;

import com.example.ironplan.model.CategoriaIpaq;
import com.example.ironplan.model.ClasificacionSus;
import com.example.ironplan.model.ExperimentoRetoEstado;
import com.example.ironplan.model.IpaqCorte;
import com.example.ironplan.model.ParticipanteCategoria;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public final class ExperimentoDTOs {

    private ExperimentoDTOs() {}

    public record InscripcionRequest(
            @NotNull ParticipanteCategoria categoria,
            @NotBlank @Size(max = 10) String objetivoCodigo,
            @Size(max = 500) String objetivoTextoLibre
    ) {}

    public record InscripcionResponse(Long participanteRetoId) {}

    public record ConsentimientoRequest(
            @NotNull Long participanteRetoId,
            @NotNull Boolean acepto,
            String ipDispositivo
    ) {}

    public record IpaqRequestDto(
            @NotNull Long participanteRetoId,
            @NotNull IpaqCorte corte,
            @Min(0) @Max(7) Integer caminataDiasSemana,
            @Min(0) Integer caminataMinDia,
            @Min(0) @Max(7) Integer moderadaDiasSemana,
            @Min(0) Integer moderadaMinDia,
            @Min(0) @Max(7) Integer vigorosaDiasSemana,
            @Min(0) Integer vigorosaMinDia
    ) {}

    public record IpaqResultadoDto(
            BigDecimal metCaminata,
            BigDecimal metModerada,
            BigDecimal metVigorosa,
            BigDecimal metTotal,
            CategoriaIpaq categoriaIpaq,
            boolean esOutlier
    ) {}

    public record IpaqSubmitResponse(
            BigDecimal metTotal,
            CategoriaIpaq categoriaIpaq,
            boolean esOutlier
    ) {}

    public record IpaqRespuestaView(
            IpaqCorte corte,
            Integer caminataDiasSemana,
            Integer caminataMinDia,
            Integer moderadaDiasSemana,
            Integer moderadaMinDia,
            Integer vigorosaDiasSemana,
            Integer vigorosaMinDia,
            BigDecimal metTotalSemana,
            CategoriaIpaq categoriaIpaq,
            boolean esOutlier,
            LocalDateTime fechaAplicacion
    ) {}

    public record IpaqPairResponse(IpaqRespuestaView pre, IpaqRespuestaView post) {}

    public record SusRequestDto(
            @NotNull Long participanteRetoId,
            @Min(1) @Max(5) int susQ1,
            @Min(1) @Max(5) int susQ2,
            @Min(1) @Max(5) int susQ3,
            @Min(1) @Max(5) int susQ4,
            @Min(1) @Max(5) int susQ5,
            @Min(1) @Max(5) int susQ6,
            @Min(1) @Max(5) int susQ7,
            @Min(1) @Max(5) int susQ8,
            @Min(1) @Max(5) int susQ9,
            @Min(1) @Max(5) int susQ10
    ) {}

    public record SusSubmitResponse(BigDecimal puntajeSus, ClasificacionSus clasificacion) {}

    public record SusResumenResponse(
            BigDecimal promedioSus,
            ClasificacionSus clasificacionPromedio,
            int n,
            Map<String, Long> distribucion
    ) {}

    public record ParticipanteStatusResponse(
            Long participanteRetoId,
            Long retoId,
            boolean tieneConsentimiento,
            boolean completoPretest,
            boolean completoPosttest,
            boolean completoSus,
            boolean activo,
            boolean posttestIpaqActivo,
            boolean susActivo,
            ParticipanteCategoria categoria,
            String objetivoCodigo
    ) {}

    public record ExperimentoEstadoResponse(
            Long id,
            String nombre,
            ExperimentoRetoEstado estado,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer semanasIntervencion,
            Long competitionId,
            long participantesInscritos,
            long completaronPretest,
            long completaronPosttest,
            long completaronSus,
            long participantesActivos,
            boolean posttestIpaqActivo,
            boolean susActivo
    ) {}

    public record SnapshotGenerarResponse(int semanaGenerada, int usuariosProcesados) {}

    public record RetoResumenResponse(
            Long id,
            String nombre,
            ExperimentoRetoEstado estado,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Long organizacionId,
            String organizacionNombre,
            Long competitionId
    ) {}

    public record CreateRetoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @Size(max = 2000) String descripcion,
            @NotNull Long organizacionId,
            @NotNull LocalDate fechaInicio,
            @NotNull LocalDate fechaFin,
            @Min(1) @Max(52) Integer semanasIntervencion,
            Long competitionId
    ) {}

    /** competitionId null = desvincular */
    public record VincularCompetitionRequest(Long competitionId) {}
}
