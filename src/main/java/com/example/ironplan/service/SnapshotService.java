package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final ExperimentoRetoRepository retoRepo;
    private final ParticipanteRetoRepository participanteRepo;
    private final SnapshotSemanalUsuarioRepository snapshotRepo;
    private final ProgressRepository progressRepo;
    private final FreeActivitySessionRepository freeActivityRepo;
    private final CompetitionMemberParticipantRepository memberParticipantRepo;
    private final UserXpEventRepository xpEventRepo;

    @Transactional(readOnly = true)
    public List<SnapshotSemanalUsuario> listSnapshots(Long retoId, Long usuarioId) {
        return snapshotRepo.findByRetoIdAndUsuarioIdOrderByNumeroSemanaAsc(retoId, usuarioId);
    }

    @Transactional
    public void procesarRetosActivos() {
        for (ExperimentoReto reto : retoRepo.findByEstado(ExperimentoRetoEstado.ACTIVO)) {
            try {
                generarSnapshotSemanaActual(reto.getId(), null);
            } catch (Exception e) {
                log.error("Error generando snapshot para reto {}", reto.getId(), e);
            }
        }
    }

    /** Genera la semana en curso (job automático / compatibilidad). */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse generarSnapshotSemanaActual(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        int semana = calcularNumeroSemana(reto.getFechaInicio(), LocalDate.now());
        if (semana < 1 || semana > semanasIntervencion(reto)) {
            return new ExperimentoDTOs.SnapshotGenerarResponse(Math.max(semana, 0), 0);
        }
        int procesados = generarSnapshotSemanaSiFalta(reto, semana);
        return new ExperimentoDTOs.SnapshotGenerarResponse(semana, procesados);
    }

    /** Admin: genera snapshots faltantes hasta la semana actual o todas si el reto ya terminó. */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse generarSnapshotsFaltantes(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        int hasta = calcularSemanaHasta(reto);
        return generarSemanas(reto, 1, hasta);
    }

    /** Al cerrar el reto: genera retroactivamente todas las semanas de intervención. */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse generarSnapshotsRetroactivos(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        return generarSemanas(reto, 1, semanasIntervencion(reto));
    }

    private ExperimentoDTOs.SnapshotGenerarResponse generarSemanas(ExperimentoReto reto, int desde, int hasta) {
        if (hasta < desde) {
            return new ExperimentoDTOs.SnapshotGenerarResponse(0, 0);
        }
        int procesados = 0;
        int ultimaSemana = 0;
        for (int w = desde; w <= hasta; w++) {
            int c = generarSnapshotSemanaSiFalta(reto, w);
            if (c > 0) {
                procesados += c;
                ultimaSemana = w;
            }
        }
        if (ultimaSemana == 0) {
            ultimaSemana = hasta;
        }
        return new ExperimentoDTOs.SnapshotGenerarResponse(ultimaSemana, procesados);
    }

    private int calcularSemanaHasta(ExperimentoReto reto) {
        int semanas = semanasIntervencion(reto);
        LocalDate hoy = LocalDate.now();

        if (reto.getEstado() == ExperimentoRetoEstado.CERRADO || !hoy.isBefore(reto.getFechaFin().plusDays(1))) {
            return semanas;
        }
        if (hoy.isBefore(reto.getFechaInicio())) {
            return 0;
        }
        int actual = calcularNumeroSemana(reto.getFechaInicio(), hoy);
        return Math.min(actual, semanas);
    }

    private int generarSnapshotSemanaSiFalta(ExperimentoReto reto, int numeroSemana) {
        LocalDate inicioSemana = reto.getFechaInicio().plusWeeks(numeroSemana - 1);
        LocalDate finSemana = inicioSemana.plusDays(6);
        if (finSemana.isAfter(reto.getFechaFin())) {
            finSemana = reto.getFechaFin();
        }

        int procesados = 0;
        for (ParticipanteReto p : participanteRepo.findByRetoIdAndActivoTrue(reto.getId())) {
            if (snapshotRepo.findByRetoIdAndUsuarioIdAndNumeroSemana(
                    reto.getId(), p.getUsuario().getId(), numeroSemana).isPresent()) {
                continue;
            }
            snapshotRepo.save(buildSnapshot(reto, p, numeroSemana, inicioSemana, finSemana));
            procesados++;
        }
        return procesados;
    }

    private ExperimentoReto findRetoOrThrow(Long retoId) {
        return retoRepo.findById(retoId)
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado"));
    }

    private static int semanasIntervencion(ExperimentoReto reto) {
        return reto.getSemanasIntervencion() != null ? reto.getSemanasIntervencion() : 8;
    }

    private SnapshotSemanalUsuario buildSnapshot(
            ExperimentoReto reto,
            ParticipanteReto participante,
            int numeroSemana,
            LocalDate inicioSemana,
            LocalDate finSemana
    ) {
        User user = participante.getUsuario();
        LocalDateTime start = inicioSemana.atStartOfDay();
        LocalDateTime end = finSemana.plusDays(1).atStartOfDay();

        List<WorkoutSet> sets = progressRepo.findCompletedSetsInDateRange(user.getId(), start, end);

        int sesiones = (int) sets.stream()
                .map(s -> s.getWorkoutExercise().getWorkoutSession().getId())
                .distinct()
                .count();

        BigDecimal volumen = sets.stream()
                .map(s -> s.getVolumenSerie() != null
                        ? s.getVolumenSerie()
                        : BigDecimal.valueOf(
                                (s.getWeightKg() != null ? s.getWeightKg() : 0)
                                        * (s.getReps() != null ? s.getReps() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<BigDecimal> oneRms = sets.stream()
                .map(WorkoutSet::getOneRmEstimado)
                .filter(v -> v != null)
                .toList();

        BigDecimal oneRmProm = oneRms.isEmpty() ? null :
                oneRms.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(oneRms.size()), 2, RoundingMode.HALF_UP);
        BigDecimal oneRmMax = oneRms.stream().max(BigDecimal::compareTo).orElse(null);

        int xpAlFin = user.getLifetimeXp() != null ? user.getLifetimeXp() : 0;
        int xpSemana = sumXpEnSemana(user.getId(), start, end);

        Integer posicion = obtenerPosicionLeaderboard(reto, user.getId());

        var cardio = calcularCardio(user.getId(), start, end);

        return SnapshotSemanalUsuario.builder()
                .reto(reto)
                .usuario(user)
                .numeroSemana(numeroSemana)
                .fechaInicioSemana(inicioSemana)
                .fechaFinSemana(finSemana)
                .sesionesCompletadas(sesiones)
                .volumenTotalSemana(volumen)
                .oneRmPromedio(oneRmProm)
                .oneRmMaximo(oneRmMax)
                .xpAcumuladoAlFin(xpAlFin)
                .xpGanadoSemana(xpSemana)
                .posicionLeaderboard(posicion)
                .sesionesCardio(cardio.sesiones)
                .minutosCardio(cardio.minutos)
                .kmCardio(cardio.km)
                .build();
    }

    private record CardioAgg(int sesiones, int minutos, BigDecimal km) {}

    private CardioAgg calcularCardio(Long userId, LocalDateTime start, LocalDateTime end) {
        var sessions = freeActivityRepo.findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(userId, start, end);
        int count = sessions.size();
        int secs = sessions.stream()
                .mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                .sum();
        BigDecimal km = sessions.stream()
                .map(s -> s.getDistanceKm() != null ? BigDecimal.valueOf(s.getDistanceKm()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CardioAgg(count, secs / 60, km.setScale(2, RoundingMode.HALF_UP));
    }

    private int sumXpEnSemana(Long userId, LocalDateTime start, LocalDateTime end) {
        return xpEventRepo.findByUser_IdAndCreatedAtBetween(userId, start, end).stream()
                .filter(e -> e.getXpDelta() != null && e.getXpDelta() > 0)
                .mapToInt(UserXpEvent::getXpDelta)
                .sum();
    }

    private Integer obtenerPosicionLeaderboard(ExperimentoReto reto, Long userId) {
        if (reto.getCompetition() == null) return null;
        return memberParticipantRepo.findByCompetitionIdAndUserId(reto.getCompetition().getId(), userId)
                .map(CompetitionMemberParticipant::getRank)
                .orElse(null);
    }

    static int calcularNumeroSemana(LocalDate fechaInicio, LocalDate fecha) {
        if (fecha.isBefore(fechaInicio)) return 0;
        long days = ChronoUnit.DAYS.between(fechaInicio, fecha);
        return (int) (days / 7) + 1;
    }

    @Transactional
    public void aplicarMortalidadExperimental() {
        LocalDate hoy = LocalDate.now();
        for (ExperimentoReto reto : retoRepo.findByEstado(ExperimentoRetoEstado.ACTIVO)) {
            LocalDate limite = hoy.minusWeeks(2);
            for (ParticipanteReto p : participanteRepo.findByRetoIdAndActivoTrue(reto.getId())) {
                Optional<LocalDate> ultima = progressRepo.findWorkoutDates(p.getUsuario().getId()).stream().findFirst();
                if (ultima.isEmpty() || ultima.get().isBefore(limite)) {
                    p.setActivo(false);
                    participanteRepo.save(p);
                    log.info("Mortalidad experimental: participante {} desactivado en reto {}", p.getId(), reto.getId());
                }
            }
        }
    }
}
