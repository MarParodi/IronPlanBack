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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final UserAchievementRepository achievementRepo;
    private final RetoPointsScoringService retoPointsScoringService;

    /** Qué semanas se recalculan: todas, o solo las faltantes y las que no estaban completas. */
    enum Modo { TODAS, PENDIENTES }

    @Transactional(readOnly = true)
    public List<SnapshotSemanalUsuario> listSnapshots(Long retoId, Long usuarioId) {
        return snapshotRepo.findByRetoIdAndUsuarioIdOrderByNumeroSemanaAsc(retoId, usuarioId);
    }

    @Transactional
    public void procesarRetosActivos() {
        for (ExperimentoReto reto : retoRepo.findByEstado(ExperimentoRetoEstado.ACTIVO)) {
            try {
                generarSemanas(reto, 1, semanasTranscurridas(reto), Modo.TODAS);
            } catch (Exception e) {
                log.error("Error generando snapshot para reto {}", reto.getId(), e);
            }
        }
    }

    /** Recalcula la semana en curso (compatibilidad). */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse generarSnapshotSemanaActual(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        int semana = calcularNumeroSemana(reto.getFechaInicio(), LocalDate.now());
        if (semana < 1 || semana > semanasIntervencion(reto)) {
            return new ExperimentoDTOs.SnapshotGenerarResponse(Math.max(semana, 0), 0);
        }
        return generarSemanas(reto, semana, semana, Modo.TODAS);
    }

    /** Admin: recalcula todas las semanas hasta la actual, o todas si el reto ya terminó. */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse generarSnapshotsFaltantes(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        return generarSemanas(reto, 1, semanasTranscurridas(reto), Modo.TODAS);
    }

    /** Antes de exportar: genera las semanas faltantes y recalcula las que no estaban completas. */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse actualizarSnapshotsPendientes(Long retoId) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        return generarSemanas(reto, 1, semanasTranscurridas(reto), Modo.PENDIENTES);
    }

    /** Al cerrar el reto: recalcula todas las semanas de intervención. */
    @Transactional
    public ExperimentoDTOs.SnapshotGenerarResponse generarSnapshotsRetroactivos(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        return generarSemanas(reto, 1, semanasIntervencion(reto), Modo.TODAS);
    }

    /** Semanas de intervención ya iniciadas (todas si el reto terminó o está cerrado). */
    public int semanasTranscurridas(ExperimentoReto reto) {
        int semanas = semanasIntervencion(reto);
        LocalDate hoy = LocalDate.now();
        if (reto.getEstado() == ExperimentoRetoEstado.CERRADO || hoy.isAfter(reto.getFechaFin())) {
            return semanas;
        }
        if (hoy.isBefore(reto.getFechaInicio())) {
            return 0;
        }
        return Math.min(calcularNumeroSemana(reto.getFechaInicio(), hoy), semanas);
    }

    private ExperimentoDTOs.SnapshotGenerarResponse generarSemanas(
            ExperimentoReto reto, int desde, int hasta, Modo modo) {
        if (hasta < desde) {
            return new ExperimentoDTOs.SnapshotGenerarResponse(0, 0);
        }
        List<ParticipanteReto> participantes = participanteRepo.findByRetoIdWithUsuario(reto.getId());
        int procesados = 0;
        for (int w = desde; w <= hasta; w++) {
            procesados += generarSemana(reto, participantes, w, modo);
        }
        return new ExperimentoDTOs.SnapshotGenerarResponse(hasta, procesados);
    }

    private int generarSemana(ExperimentoReto reto, List<ParticipanteReto> participantes, int numeroSemana, Modo modo) {
        LocalDate inicioSemana = reto.getFechaInicio().plusWeeks(numeroSemana - 1);
        LocalDate finSemana = inicioSemana.plusDays(6);
        if (finSemana.isAfter(reto.getFechaFin())) {
            finSemana = reto.getFechaFin();
        }
        LocalDate hoy = LocalDate.now();

        Map<Long, SnapshotSemanalUsuario> existentes = new LinkedHashMap<>();
        List<ParticipanteReto> aProcesar = new java.util.ArrayList<>();
        for (ParticipanteReto p : participantes) {
            Long userId = p.getUsuario().getId();
            Optional<SnapshotSemanalUsuario> previo =
                    snapshotRepo.findByRetoIdAndUsuarioIdAndNumeroSemana(reto.getId(), userId, numeroSemana);
            if (modo == Modo.PENDIENTES && previo.isPresent()
                    && Boolean.TRUE.equals(previo.get().getSemanaCompleta())) {
                continue;
            }
            previo.ifPresent(s -> existentes.put(userId, s));
            aProcesar.add(p);
        }
        if (aProcesar.isEmpty()) return 0;

        List<Long> userIds = aProcesar.stream().map(p -> p.getUsuario().getId()).toList();
        Map<Long, Double> puntosAcum = puntosReto(reto, userIds, finSemana);
        Map<Long, Double> puntosPrev = puntosReto(reto, userIds, inicioSemana.minusDays(1));

        for (ParticipanteReto p : aProcesar) {
            Long userId = p.getUsuario().getId();
            DatosSemana datos = cargarDatos(reto, p, inicioSemana, finSemana, puntosAcum, puntosPrev);
            SnapshotSemanalUsuario nuevo = construirSnapshot(reto, p, numeroSemana, inicioSemana, finSemana, hoy, datos);
            SnapshotSemanalUsuario previo = existentes.get(userId);
            if (previo != null) {
                nuevo.setId(previo.getId());
                nuevo.setCreatedAt(previo.getCreatedAt());
            }
            snapshotRepo.save(nuevo);
        }
        return aProcesar.size();
    }

    /** Puntos TEAM_POINTS acumulados de cada usuario hasta {@code hasta}; null si el reto no tiene esa competencia. */
    private Map<Long, Double> puntosReto(ExperimentoReto reto, List<Long> userIds, LocalDate hasta) {
        Competition c = reto.getCompetition();
        if (c == null || c.getMetricType() != MetricType.TEAM_POINTS) return null;
        LocalDate inicio = c.getStartDate();
        LocalDate fin = hasta;
        if (c.getEndDate() != null && c.getEndDate().isBefore(fin)) fin = c.getEndDate();
        if (inicio == null || fin.isBefore(inicio)) {
            Map<Long, Double> ceros = new LinkedHashMap<>();
            for (Long id : userIds) ceros.put(id, 0.0);
            return ceros;
        }
        return retoPointsScoringService.scoreUsers(userIds, inicio, fin);
    }

    record DatosSemana(
            List<WorkoutSet> sets,
            List<FreeActivitySession> libres,
            List<UserXpEvent> xpSemana,
            List<UserXpEvent> xpPeriodo,
            List<UserXpEvent> xpPosterior,
            long logrosSemana,
            long logrosAcumulados,
            Double puntosAcumulados,
            Double puntosPrevios,
            Integer posicionLeaderboard
    ) {}

    private DatosSemana cargarDatos(
            ExperimentoReto reto,
            ParticipanteReto participante,
            LocalDate inicioSemana,
            LocalDate finSemana,
            Map<Long, Double> puntosAcum,
            Map<Long, Double> puntosPrev
    ) {
        Long userId = participante.getUsuario().getId();
        LocalDateTime start = inicioSemana.atStartOfDay();
        LocalDateTime end = finSemana.plusDays(1).atStartOfDay();
        LocalDateTime inicioReto = reto.getFechaInicio().atStartOfDay();

        return new DatosSemana(
                progressRepo.findCompletedSetsInDateRange(userId, start, end),
                freeActivityRepo.findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(userId, start, end),
                xpEventRepo.findByUser_IdAndCreatedAtBetween(userId, start, end),
                xpEventRepo.findByUser_IdAndCreatedAtBetween(userId, inicioReto, end),
                xpEventRepo.findByUser_IdAndCreatedAtGreaterThanEqual(userId, end),
                achievementRepo.countByUser_IdAndUnlockedAtGreaterThanEqualAndUnlockedAtLessThan(userId, start, end),
                achievementRepo.countByUser_IdAndUnlockedAtGreaterThanEqualAndUnlockedAtLessThan(userId, inicioReto, end),
                puntosAcum != null ? puntosAcum.get(userId) : null,
                puntosPrev != null ? puntosPrev.get(userId) : null,
                obtenerPosicionLeaderboard(reto, userId)
        );
    }

    SnapshotSemanalUsuario construirSnapshot(
            ExperimentoReto reto,
            ParticipanteReto participante,
            int numeroSemana,
            LocalDate inicioSemana,
            LocalDate finSemana,
            LocalDate hoy,
            DatosSemana d
    ) {
        User user = participante.getUsuario();

        Map<Long, WorkoutSession> sesionesFuerza = new LinkedHashMap<>();
        for (WorkoutSet s : d.sets()) {
            WorkoutSession ws = s.getWorkoutExercise().getWorkoutSession();
            sesionesFuerza.putIfAbsent(ws.getId(), ws);
        }
        int sesiones = sesionesFuerza.size();

        int minutosFuerza = (int) sesionesFuerza.values().stream()
                .filter(ws -> ws.getStartedAt() != null && ws.getCompletedAt() != null)
                .mapToLong(ws -> Math.max(0, Duration.between(ws.getStartedAt(), ws.getCompletedAt()).toMinutes()))
                .sum();

        BigDecimal volumen = d.sets().stream()
                .map(s -> s.getVolumenSerie() != null
                        ? s.getVolumenSerie()
                        : BigDecimal.valueOf(
                                (s.getWeightKg() != null ? s.getWeightKg() : 0)
                                        * (s.getReps() != null ? s.getReps() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<BigDecimal> oneRms = d.sets().stream()
                .map(WorkoutSet::getOneRmEstimado)
                .filter(v -> v != null)
                .toList();
        BigDecimal oneRmProm = oneRms.isEmpty() ? null :
                oneRms.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(oneRms.size()), 2, RoundingMode.HALF_UP);
        BigDecimal oneRmMax = oneRms.stream().max(BigDecimal::compareTo).orElse(null);

        List<Double> cargas = d.sets().stream()
                .map(WorkoutSet::getWeightKg)
                .filter(w -> w != null && w > 0)
                .toList();
        BigDecimal cargaPromedio = cargas.isEmpty() ? null :
                BigDecimal.valueOf(cargas.stream().mapToDouble(Double::doubleValue).average().orElse(0))
                        .setScale(2, RoundingMode.HALF_UP);

        int sesionesLibre = d.libres().size();
        int segundosLibre = d.libres().stream()
                .mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                .sum();
        BigDecimal km = d.libres().stream()
                .map(s -> s.getDistanceKm() != null ? BigDecimal.valueOf(s.getDistanceKm()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Set<LocalDate> dias = new HashSet<>();
        sesionesFuerza.values().stream()
                .map(ws -> ws.getCompletedAt() != null ? ws.getCompletedAt() : ws.getStartedAt())
                .filter(t -> t != null)
                .forEach(t -> dias.add(t.toLocalDate()));
        d.libres().stream()
                .map(FreeActivitySession::getCompletedAt)
                .filter(t -> t != null)
                .forEach(t -> dias.add(t.toLocalDate()));

        int sesionesTotales = sesiones + sesionesLibre;
        BigDecimal previstas = sesionesPrevistas(user.getTrainDays(), inicioSemana, finSemana);
        BigDecimal adherencia = previstas == null || previstas.signum() == 0 ? null :
                BigDecimal.valueOf(sesionesTotales * 100.0)
                        .divide(previstas, 2, RoundingMode.HALF_UP);

        int xpSemana = sumaPositiva(d.xpSemana());
        int xpPeriodo = sumaPositiva(d.xpPeriodo());
        int xpPosterior = d.xpPosterior().stream()
                .mapToInt(e -> e.getXpDelta() != null ? e.getXpDelta() : 0)
                .sum();
        int lifetimeActual = user.getLifetimeXp() != null ? user.getLifetimeXp() : 0;
        int lifetimeAlFin = Math.max(0, lifetimeActual - xpPosterior);

        BigDecimal puntosAcum = d.puntosAcumulados() == null ? null :
                BigDecimal.valueOf(d.puntosAcumulados()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal puntosSemana = d.puntosAcumulados() == null ? null :
                BigDecimal.valueOf(d.puntosAcumulados() - (d.puntosPrevios() != null ? d.puntosPrevios() : 0))
                        .setScale(2, RoundingMode.HALF_UP);

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
                .xpAcumuladoAlFin(lifetimeAlFin)
                .xpGanadoSemana(xpSemana)
                .posicionLeaderboard(d.posicionLeaderboard())
                .sesionesCardio(sesionesLibre)
                .minutosCardio(segundosLibre / 60)
                .kmCardio(km)
                .sesionesTotales(sesionesTotales)
                .sesionesPrevistas(previstas)
                .adherenciaPct(adherencia)
                .diasActivos(dias.size())
                .minutosFuerza(minutosFuerza)
                .cargaPromedio(cargaPromedio)
                .xpAcumuladoPeriodo(xpPeriodo)
                .rangoFinSemana(XpRank.fromLifetimeXp(lifetimeAlFin))
                .logrosSemana((int) d.logrosSemana())
                .logrosAcumulados((int) d.logrosAcumulados())
                .puntosRetoSemana(puntosSemana)
                .puntosRetoAcumulados(puntosAcum)
                .semanaCompleta(finSemana.isBefore(hoy))
                .activo(!Boolean.FALSE.equals(participante.getActivo()))
                .build();
    }

    /** Días previstos por semana, prorrateados si la semana está recortada por la fecha de fin. */
    static BigDecimal sesionesPrevistas(Integer trainDays, LocalDate inicio, LocalDate fin) {
        if (trainDays == null || trainDays <= 0) return null;
        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        return BigDecimal.valueOf(trainDays * dias / 7.0).setScale(2, RoundingMode.HALF_UP);
    }

    private static int sumaPositiva(List<UserXpEvent> eventos) {
        return eventos.stream()
                .filter(e -> e.getXpDelta() != null && e.getXpDelta() > 0)
                .mapToInt(UserXpEvent::getXpDelta)
                .sum();
    }

    private ExperimentoReto findRetoOrThrow(Long retoId) {
        return retoRepo.findById(retoId)
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado"));
    }

    private static int semanasIntervencion(ExperimentoReto reto) {
        return reto.getSemanasIntervencion() != null ? reto.getSemanasIntervencion() : 8;
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
        LocalDate limite = LocalDate.now().minusWeeks(2);
        for (ExperimentoReto reto : retoRepo.findByEstado(ExperimentoRetoEstado.ACTIVO)) {
            for (ParticipanteReto p : participanteRepo.findByRetoIdAndActivoTrue(reto.getId())) {
                Long userId = p.getUsuario().getId();
                Optional<LocalDate> ultimaFuerza = progressRepo.findWorkoutDates(userId).stream().findFirst();
                Optional<LocalDate> ultimaLibre = freeActivityRepo.findByUser_IdOrderByCompletedAtDesc(userId).stream()
                        .map(FreeActivitySession::getCompletedAt)
                        .filter(t -> t != null)
                        .map(LocalDateTime::toLocalDate)
                        .findFirst();
                LocalDate ultima = ultimaFuerza.orElse(null);
                if (ultimaLibre.isPresent() && (ultima == null || ultimaLibre.get().isAfter(ultima))) {
                    ultima = ultimaLibre.get();
                }
                if (ultima == null || ultima.isBefore(limite)) {
                    p.setActivo(false);
                    participanteRepo.save(p);
                    log.info("Mortalidad experimental: participante {} desactivado en reto {}", p.getId(), reto.getId());
                }
            }
        }
    }
}
