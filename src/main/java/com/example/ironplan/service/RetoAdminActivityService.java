package com.example.ironplan.service;

import com.example.ironplan.config.RetoPointsProperties;
import com.example.ironplan.model.FreeActivityType;
import com.example.ironplan.model.MetricType;
import com.example.ironplan.model.RetoActivityReview;
import com.example.ironplan.model.RetoActivityReviewFlag;
import com.example.ironplan.model.RetoActivityReviewStatus;
import com.example.ironplan.model.RetoActivitySource;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.FreeActivitySessionRepository;
import com.example.ironplan.repository.RetoActivityReviewRepository;
import com.example.ironplan.repository.UserActivityRepository;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.repository.WorkoutSessionRepository;
import com.example.ironplan.repository.WorkoutSetRepository;
import com.example.ironplan.repository.projection.ActividadLibreDetalle;
import com.example.ironplan.repository.projection.MetricaDiariaUsuario;
import com.example.ironplan.repository.projection.OneRmDiarioUsuario;
import com.example.ironplan.repository.projection.SesionFuerzaDetalle;
import com.example.ironplan.rest.dto.RetoAdminActivityDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Historial de aportes y cola de revisión administrativa. No modifica el scoring:
 * reconstruye el desglose con las mismas reglas y persiste solo etiquetas/notas.
 */
@Service
@RequiredArgsConstructor
public class RetoAdminActivityService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("d MMM", new Locale("es", "CL"));
    private static final int DUPLICATE_GAP_MINUTES = 15;
    private static final int DURATION_MISMATCH_SECONDS = 120;
    private static final int EXCESSIVE_HOURS_MINUTES = 180;
    private static final int EXTREME_MINUTES = 240;

    private final CompetitionService competitionService;
    private final FreeActivitySessionRepository freeActivityRepo;
    private final WorkoutSessionRepository workoutSessionRepo;
    private final UserActivityRepository activityRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final RetoActivityReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final OrganizationalAccessService accessService;
    private final RetoPointsProperties props;

    @Transactional(readOnly = true)
    public RetoAdminActivityDTOs.PointHistory getPointHistory(Long competitionId, Long userId) {
        CompetitionService.RetoAdminScope scope = competitionService.requireRetoAdminScope(competitionId);
        User user = scope.findUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("El usuario no participa en este reto");
        }
        LocalDateTime desde = scope.start().atStartOfDay();
        LocalDateTime hasta = scope.end().atTime(LocalTime.MAX);
        List<Long> ids = List.of(userId);
        List<ActividadLibreDetalle> libres = freeActivityRepo.findDetalleForUsers(ids, desde, hasta);
        List<SesionFuerzaDetalle> fuerzas = workoutSessionRepo.findDetalleForUsers(ids, desde, hasta);
        List<MetricaDiariaUsuario> volumen = activityRepo.sumDailyMetricByUser(
                ids, MetricType.VOLUME_TOTAL, scope.start(), scope.end());
        List<OneRmDiarioUsuario> oneRm = workoutSetRepo.findDailyMaxOneRmForUsers(ids, scope.start(), scope.end());
        Map<SourceKey, RetoActivityReview> reviews = indexReviews(scope.competition().getId());
        return construirHistorial(
                userId, fullName(user), scope.start(), scope.end(), LocalDate.now(),
                libres, fuerzas, volumen, oneRm, reviews);
    }

    @Transactional(readOnly = true)
    public RetoAdminActivityDTOs.ActivityReviewQueue listReviews(Long competitionId, RetoActivityReviewFlag flag) {
        CompetitionService.RetoAdminScope scope = competitionService.requireRetoAdminScope(competitionId);
        if (scope.userIds().isEmpty()) {
            return RetoAdminActivityDTOs.ActivityReviewQueue.builder().items(List.of()).build();
        }
        LocalDateTime desde = scope.start().atStartOfDay();
        LocalDateTime hasta = scope.end().atTime(LocalTime.MAX);
        List<ActividadLibreDetalle> libres = freeActivityRepo.findDetalleForUsers(scope.userIds(), desde, hasta);
        List<SesionFuerzaDetalle> fuerzas = workoutSessionRepo.findDetalleForUsers(scope.userIds(), desde, hasta);
        Map<SourceKey, RetoActivityReview> reviews = indexReviews(scope.competition().getId());
        Map<Long, String> names = scope.roster().stream()
                .collect(Collectors.toMap(User::getId, RetoAdminActivityService::fullName, (a, b) -> a));
        List<RetoAdminActivityDTOs.ActivityReviewItem> items =
                construirCola(libres, fuerzas, reviews, names);
        if (flag != null) {
            items = items.stream()
                    .filter(i -> i.getSuggestedFlags().contains(flag) || i.getAdminFlags().contains(flag))
                    .toList();
        }
        return RetoAdminActivityDTOs.ActivityReviewQueue.builder().items(items).build();
    }

    @Transactional
    public RetoAdminActivityDTOs.ActivityReviewItem upsertReview(
            Long competitionId, RetoAdminActivityDTOs.UpsertReviewRequest req) {
        CompetitionService.RetoAdminScope scope = competitionService.requireRetoAdminScope(competitionId);
        if (req.getSource() == null || req.getSourceId() == null) {
            throw new IllegalArgumentException("Fuente e identificador de la actividad son obligatorios");
        }
        RawActivity raw = findRaw(scope, req.getSource(), req.getSourceId());
        User participant = scope.findUser(raw.userId);
        if (participant == null) {
            throw new IllegalArgumentException("El usuario no participa en este reto");
        }
        User marker = accessService.getCurrentUser();
        User markerRef = marker != null ? userRepo.findById(marker.getId()).orElse(marker) : null;

        RetoActivityReview entity = reviewRepo
                .findByCompetition_IdAndSourceAndSourceId(competitionId, req.getSource(), req.getSourceId())
                .orElseGet(RetoActivityReview::new);
        entity.setCompetition(scope.competition());
        entity.setUser(participant);
        entity.setSource(req.getSource());
        entity.setSourceId(req.getSourceId());
        entity.setFlags(serializeFlags(req.getFlags()));
        entity.setNote(req.getNote() == null || req.getNote().isBlank() ? null : req.getNote().trim());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : RetoActivityReviewStatus.PENDING_REVIEW);
        entity.setMarkedBy(markerRef);
        entity.setMarkedAt(LocalDateTime.now());
        reviewRepo.save(entity);

        List<RetoActivityReviewFlag> suggested = new ArrayList<>(raw.suggested);
        return toReviewItem(raw, entity, fullName(participant), suggested);
    }

    RetoAdminActivityDTOs.PointHistory construirHistorial(
            Long userId,
            String fullName,
            LocalDate start,
            LocalDate end,
            LocalDate hoy,
            List<ActividadLibreDetalle> libres,
            List<SesionFuerzaDetalle> fuerzas,
            List<MetricaDiariaUsuario> volumen,
            List<OneRmDiarioUsuario> oneRm,
            Map<SourceKey, RetoActivityReview> reviews
    ) {
        List<RawActivity> raws = clasificar(libres, fuerzas);
        raws.sort(Comparator.comparing(r -> r.completedAt != null ? r.completedAt : r.startedAt));

        int semanas = (int) (ChronoUnit.DAYS.between(start, end) / 7) + 1;
        double[] ponderadosSemana = new double[semanas];
        Map<LocalDate, List<RawActivity>> porDia = new LinkedHashMap<>();
        for (RawActivity r : raws) {
            porDia.computeIfAbsent(r.dia(), k -> new ArrayList<>()).add(r);
        }

        Map<LocalDate, Double> constanciaPorDia = new HashMap<>();
        for (var entry : porDia.entrySet()) {
            LocalDate dia = entry.getKey();
            List<RawActivity> delDia = entry.getValue();
            List<RawActivity> validas = delDia.stream().filter(a -> a.valid).toList();
            List<RawActivity> puntuables = validas.stream()
                    .sorted(Comparator.comparingDouble((RawActivity a) -> a.esfuerzo).reversed())
                    .limit(props.getMaxActividadesDia())
                    .toList();
            Set<RawActivity> top = Set.copyOf(puntuables);

            double rawFuerza = 0;
            double rawLibre = 0;
            double ponderados = 0;
            for (RawActivity a : puntuables) {
                if (a.source == RetoActivitySource.WORKOUT) rawFuerza += a.esfuerzo;
                else rawLibre += a.esfuerzo;
                ponderados += a.minutosPonderados;
            }

            double constancia = puntuables.isEmpty() ? 0 : props.getConstanciaPorDia();
            double uncapped = constancia + rawFuerza + rawLibre;
            double capped = Math.min(props.getTopeDiario(), uncapped);
            double scale = uncapped <= 0 ? 0 : capped / uncapped;

            for (RawActivity a : delDia) {
                if (!a.valid) {
                    a.points = 0;
                    a.validationStatus = "NO_PUNTUA";
                    a.ruleApplied = a.invalidReason;
                } else if (!top.contains(a)) {
                    a.points = 0;
                    a.validationStatus = "NO_PUNTUA";
                    a.ruleApplied = "No puntúa: máximo " + props.getMaxActividadesDia() + " actividades por día";
                } else {
                    a.points = a.esfuerzo * scale;
                    a.validationStatus = "PUNTUADA";
                    if (scale < 1.0 - 1e-9) {
                        a.ruleApplied = a.ruleBase + "; tope diario " + strip(props.getTopeDiario())
                                + " pts (escala " + strip(scale) + ")";
                    } else {
                        a.ruleApplied = a.ruleBase;
                    }
                }
            }

            int semana = indiceSemana(start, dia, semanas);
            ponderadosSemana[semana] += ponderados;
            constanciaPorDia.put(dia, constancia * scale);
        }

        double[] volumenSemanal = new double[semanas];
        for (var v : volumen) {
            if (v.userId() == null || !v.userId().equals(userId) || v.fecha() == null) continue;
            volumenSemanal[indiceSemana(start, v.fecha(), semanas)] += v.valor() != null ? v.valor() : 0;
        }
        double[] oneRmSemanal = new double[semanas];
        for (var r : oneRm) {
            if (r.userId() == null || !r.userId().equals(userId) || r.fecha() == null || r.oneRm() == null) continue;
            int i = indiceSemana(start, r.fecha(), semanas);
            oneRmSemanal[i] = Math.max(oneRmSemanal[i], r.oneRm().doubleValue());
        }

        Map<Integer, double[]> progresoPorSemana = new LinkedHashMap<>();
        double total = constanciaPorDia.values().stream().mapToDouble(Double::doubleValue).sum();
        for (RawActivity a : raws) total += a.points;
        for (int semana = 0; semana < semanas; semana++) {
            double[] progreso = progresoFuerzaLibre(semana, ponderadosSemana, oneRmSemanal, volumenSemanal);
            if (progreso[0] + progreso[1] > 0) {
                progresoPorSemana.put(semana, progreso);
                total += progreso[0] + progreso[1];
            }
        }

        List<RetoAdminActivityDTOs.DayGroup> days = new ArrayList<>();
        List<LocalDate> fechas = new ArrayList<>(porDia.keySet());
        fechas.sort(Comparator.reverseOrder());
        for (LocalDate dia : fechas) {
            List<RetoAdminActivityDTOs.HistoryEntry> entries = new ArrayList<>();
            List<RawActivity> delDia = new ArrayList<>(porDia.get(dia));
            delDia.sort(Comparator.comparing((RawActivity a) ->
                    a.completedAt != null ? a.completedAt : a.startedAt).reversed());
            for (RawActivity a : delDia) {
                entries.add(toHistoryEntry(a, reviews));
            }
            double constancia = constanciaPorDia.getOrDefault(dia, 0.0);
            if (constancia > 0) {
                entries.add(RetoAdminActivityDTOs.HistoryEntry.builder()
                        .kind("CONSTANCIA")
                        .activityType("CONSTANCIA")
                        .activityLabel("Constancia")
                        .occurredAt(dia.atTime(23, 59))
                        .points(constancia)
                        .ruleApplied("10 pts por día con al menos una actividad válida")
                        .dataSource("Cálculo del reto")
                        .validationStatus("PUNTUADA")
                        .adminFlags(List.of())
                        .build());
            }
            days.add(RetoAdminActivityDTOs.DayGroup.builder()
                    .date(dia)
                    .label(dayLabel(dia, hoy))
                    .entries(entries)
                    .build());
        }

        for (int semana = semanas - 1; semana >= 0; semana--) {
            double[] progreso = progresoPorSemana.get(semana);
            if (progreso == null) continue;
            LocalDate weekStart = start.plusDays((long) semana * 7);
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(end)) weekEnd = end;
            List<RetoAdminActivityDTOs.HistoryEntry> entries = new ArrayList<>();
            if (progreso[0] > 0) {
                entries.add(progresoEntry("Progreso · fuerza", progreso[0], weekEnd,
                        "Mejora de 1RM o volumen vs baseline personal"));
            }
            if (progreso[1] > 0) {
                entries.add(progresoEntry("Progreso · actividad libre", progreso[1], weekEnd,
                        "Mejora de minutos ponderados vs baseline personal, tope semanal "
                                + strip(props.getProgresoMaxSemanal()) + " pts"));
            }
            days.add(RetoAdminActivityDTOs.DayGroup.builder()
                    .date(weekEnd)
                    .label("Semana " + (semana + 1) + " · progreso")
                    .entries(entries)
                    .build());
        }

        days.sort(Comparator
                .comparing(RetoAdminActivityDTOs.DayGroup::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(d -> d.getLabel() != null && d.getLabel().contains("progreso") ? 1 : 0));

        return RetoAdminActivityDTOs.PointHistory.builder()
                .userId(userId)
                .fullName(fullName)
                .totalPoints(total)
                .days(days)
                .build();
    }

    List<RetoAdminActivityDTOs.ActivityReviewItem> construirCola(
            List<ActividadLibreDetalle> libres,
            List<SesionFuerzaDetalle> fuerzas,
            Map<SourceKey, RetoActivityReview> reviews,
            Map<Long, String> names
    ) {
        List<RawActivity> raws = clasificar(libres, fuerzas);
        aplicarHeuristicasColectivas(raws);
        List<RetoAdminActivityDTOs.ActivityReviewItem> items = new ArrayList<>();
        for (RawActivity raw : raws) {
            RetoActivityReview review = reviews.get(new SourceKey(raw.source, raw.id));
            if (raw.suggested.isEmpty() && review == null) continue;
            items.add(toReviewItem(raw, review, names.getOrDefault(raw.userId, "Usuario"), new ArrayList<>(raw.suggested)));
        }
        items.sort(Comparator.comparing(RetoAdminActivityDTOs.ActivityReviewItem::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    private List<RawActivity> clasificar(List<ActividadLibreDetalle> libres, List<SesionFuerzaDetalle> fuerzas) {
        List<RawActivity> raws = new ArrayList<>();
        if (libres != null) {
            for (ActividadLibreDetalle a : libres) raws.add(fromLibre(a));
        }
        if (fuerzas != null) {
            for (SesionFuerzaDetalle s : fuerzas) raws.add(fromFuerza(s));
        }
        aplicarHeuristicasIndividuales(raws);
        aplicarHeuristicasColectivas(raws);
        return raws;
    }

    private RawActivity fromLibre(ActividadLibreDetalle a) {
        RawActivity r = new RawActivity();
        r.id = a.id();
        r.userId = a.userId();
        r.source = RetoActivitySource.FREE_ACTIVITY;
        r.activityType = a.activityType() != null ? a.activityType().name() : "OTRA";
        r.activityLabel = labelLibre(a.activityType(), a.activityTypeOther());
        r.startedAt = a.startedAt();
        r.completedAt = a.completedAt();
        r.durationSeconds = a.durationSeconds() != null ? a.durationSeconds() : 0;
        r.distanceKm = a.distanceKm();
        r.photoUrl = a.photoUrl();
        r.dataSource = "Actividad libre en la app";
        r.modalidadLibre = true;

        if (a.durationSeconds() == null || a.completedAt() == null) {
            r.valid = false;
            r.invalidReason = "No puntúa: datos incompletos";
            r.ruleBase = r.invalidReason;
            return r;
        }
        double minutos = a.durationSeconds() / 60.0;
        if (minutos < props.getMinutosMinimos()) {
            r.valid = false;
            r.invalidReason = "No puntúa: duración menor a " + props.getMinutosMinimos() + " min";
            r.ruleBase = r.invalidReason;
            return r;
        }
        if (props.isRequiereFotoActividadLibre() && (a.photoUrl() == null || a.photoUrl().isBlank())) {
            r.valid = false;
            r.invalidReason = "No puntúa: falta evidencia fotográfica";
            r.ruleBase = r.invalidReason;
            return r;
        }
        double puntuables = Math.min(minutos, props.getMinutosMaxPuntuables());
        double intensidad = props.intensidadDe(a.activityType());
        r.esfuerzo = (puntuables / props.getMinutosPorBloque()) * props.getPuntosPorBloque() * intensidad;
        r.minutosPonderados = puntuables * intensidad;
        r.valid = true;
        r.ruleBase = strip(props.getPuntosPorBloque()) + " pts por cada " + props.getMinutosPorBloque()
                + " min, tope " + props.getMinutosMaxPuntuables() + " min, intensidad "
                + r.activityLabel.toLowerCase(Locale.ROOT) + " ×" + strip(intensidad);
        return r;
    }

    private RawActivity fromFuerza(SesionFuerzaDetalle s) {
        RawActivity r = new RawActivity();
        r.id = s.id();
        r.userId = s.userId();
        r.source = RetoActivitySource.WORKOUT;
        r.activityType = "FUERZA";
        r.activityLabel = "Fuerza";
        r.startedAt = s.startedAt();
        r.completedAt = s.completedAt();
        r.progressPercentage = s.progressPercentage();
        r.completedExercises = s.completedExercises();
        r.dataSource = "Sesión de rutina";
        r.modalidadLibre = false;
        if (s.startedAt() != null && s.completedAt() != null) {
            r.durationSeconds = (int) Math.max(0, Duration.between(s.startedAt(), s.completedAt()).toSeconds());
        }

        if (s.startedAt() == null || s.completedAt() == null) {
            r.valid = false;
            r.invalidReason = "No puntúa: datos incompletos";
            r.ruleBase = r.invalidReason;
            return r;
        }
        if (s.completedExercises() == null || s.completedExercises() < props.getMinEjerciciosCompletados()) {
            r.valid = false;
            r.invalidReason = "No puntúa: sin trabajo efectivo de fuerza";
            r.ruleBase = r.invalidReason;
            return r;
        }
        double minutos = Duration.between(s.startedAt(), s.completedAt()).toSeconds() / 60.0;
        if (minutos < props.getMinutosMinimos()) {
            r.valid = false;
            r.invalidReason = "No puntúa: duración menor a " + props.getMinutosMinimos() + " min";
            r.ruleBase = r.invalidReason;
            return r;
        }
        double puntuables = Math.min(minutos, props.getMinutosMaxPuntuables());
        r.esfuerzo = (puntuables / props.getMinutosPorBloque()) * props.getPuntosPorBloque();
        String regla = strip(props.getPuntosPorBloque()) + " pts por cada " + props.getMinutosPorBloque()
                + " min, tope " + props.getMinutosMaxPuntuables() + " min";
        if (s.progressPercentage() != null && s.progressPercentage() >= 100.0) {
            r.esfuerzo += props.getBonoRutinaCompleta();
            regla += ", +" + strip(props.getBonoRutinaCompleta()) + " rutina completa";
        }
        r.valid = true;
        r.ruleBase = regla;
        return r;
    }

    private void aplicarHeuristicasIndividuales(List<RawActivity> raws) {
        for (RawActivity r : raws) {
            if (r.source == RetoActivitySource.FREE_ACTIVITY && (r.photoUrl == null || r.photoUrl.isBlank())) {
                r.suggested.add(RetoActivityReviewFlag.MISSING_EVIDENCE);
            }
            if (!r.valid) {
                r.suggested.add(RetoActivityReviewFlag.OUTSIDE_RULES);
            }
            int minutos = r.durationSeconds / 60;
            if (minutos > props.getMinutosMaxPuntuables() || minutos > EXCESSIVE_HOURS_MINUTES) {
                r.suggested.add(RetoActivityReviewFlag.EXCESSIVE_DURATION);
            }
            if (minutos > EXTREME_MINUTES) {
                r.suggested.add(RetoActivityReviewFlag.SUSPICIOUS);
            }
            if (r.startedAt != null && r.completedAt != null && r.completedAt.isBefore(r.startedAt)) {
                r.suggested.add(RetoActivityReviewFlag.INCONSISTENT_DATA);
            }
            if (r.source == RetoActivitySource.FREE_ACTIVITY && r.startedAt != null && r.completedAt != null) {
                long elapsed = Math.abs(Duration.between(r.startedAt, r.completedAt).toSeconds());
                if (Math.abs(elapsed - r.durationSeconds) > DURATION_MISMATCH_SECONDS) {
                    r.suggested.add(RetoActivityReviewFlag.INCONSISTENT_DATA);
                }
            }
            if (r.distanceKm != null && r.distanceKm > 0 && r.durationSeconds > 0) {
                double horas = r.durationSeconds / 3600.0;
                double kmh = r.distanceKm / horas;
                if (kmh > maxKmh(r.activityType)) {
                    r.suggested.add(RetoActivityReviewFlag.INCONSISTENT_DATA);
                }
            }
        }
    }

    private void aplicarHeuristicasColectivas(List<RawActivity> raws) {
        Map<Long, List<RawActivity>> porUsuario = raws.stream()
                .collect(Collectors.groupingBy(a -> a.userId));
        for (List<RawActivity> lista : porUsuario.values()) {
            List<RawActivity> ordenadas = new ArrayList<>(lista);
            ordenadas.sort(Comparator.comparing(a -> a.startedAt != null ? a.startedAt : a.completedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (int i = 0; i < ordenadas.size(); i++) {
                RawActivity a = ordenadas.get(i);
                for (int j = i + 1; j < ordenadas.size(); j++) {
                    RawActivity b = ordenadas.get(j);
                    if (!Objects.equals(a.activityType, b.activityType) || a.source != b.source) continue;
                    if (cercaOSolapan(a, b)) {
                        a.suggested.add(RetoActivityReviewFlag.DUPLICATE);
                        b.suggested.add(RetoActivityReviewFlag.DUPLICATE);
                    }
                }
            }
            Map<LocalDate, Long> validasPorDia = lista.stream()
                    .filter(x -> x.valid)
                    .collect(Collectors.groupingBy(RawActivity::dia, Collectors.counting()));
            for (RawActivity a : lista) {
                if (validasPorDia.getOrDefault(a.dia(), 0L) > props.getMaxActividadesDia()) {
                    a.suggested.add(RetoActivityReviewFlag.SUSPICIOUS);
                }
            }
        }
    }

    private boolean cercaOSolapan(RawActivity a, RawActivity b) {
        LocalDateTime aStart = a.startedAt != null ? a.startedAt : a.completedAt;
        LocalDateTime aEnd = a.completedAt != null ? a.completedAt : a.startedAt;
        LocalDateTime bStart = b.startedAt != null ? b.startedAt : b.completedAt;
        LocalDateTime bEnd = b.completedAt != null ? b.completedAt : b.startedAt;
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;
        boolean solapan = aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
        long gapStart = Math.abs(Duration.between(aStart, bStart).toMinutes());
        long gapEnd = Math.abs(Duration.between(aEnd, bEnd).toMinutes());
        return solapan || gapStart < DUPLICATE_GAP_MINUTES || gapEnd < DUPLICATE_GAP_MINUTES;
    }

    private double maxKmh(String activityType) {
        if (activityType == null) return 40;
        return switch (activityType) {
            case "CAMINATA", "CAMINADORA" -> 12;
            case "RUNNING" -> 25;
            case "NATACION" -> 8;
            case "BICICLETA_ESTATICA", "ELIPTICA" -> 50;
            default -> 40;
        };
    }

    private RawActivity findRaw(CompetitionService.RetoAdminScope scope, RetoActivitySource source, Long sourceId) {
        LocalDateTime desde = scope.start().atStartOfDay();
        LocalDateTime hasta = scope.end().atTime(LocalTime.MAX);
        if (source == RetoActivitySource.FREE_ACTIVITY) {
            return freeActivityRepo.findDetalleForUsers(scope.userIds(), desde, hasta).stream()
                    .filter(a -> sourceId.equals(a.id()))
                    .findFirst()
                    .map(this::fromLibre)
                    .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada en el período del reto"));
        }
        return workoutSessionRepo.findDetalleForUsers(scope.userIds(), desde, hasta).stream()
                .filter(s -> sourceId.equals(s.id()))
                .findFirst()
                .map(this::fromFuerza)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada en el período del reto"));
    }

    private Map<SourceKey, RetoActivityReview> indexReviews(Long competitionId) {
        Map<SourceKey, RetoActivityReview> map = new HashMap<>();
        for (RetoActivityReview r : reviewRepo.findByCompetition_Id(competitionId)) {
            map.put(new SourceKey(r.getSource(), r.getSourceId()), r);
        }
        return map;
    }

    private RetoAdminActivityDTOs.HistoryEntry toHistoryEntry(RawActivity a, Map<SourceKey, RetoActivityReview> reviews) {
        RetoActivityReview review = reviews.get(new SourceKey(a.source, a.id));
        String status = a.validationStatus;
        if (review != null && review.getStatus() == RetoActivityReviewStatus.PENDING_REVIEW) {
            status = "EN_REVISION";
        }
        return RetoAdminActivityDTOs.HistoryEntry.builder()
                .kind("ACTIVITY")
                .source(a.source)
                .sourceId(a.id)
                .activityType(a.activityType)
                .activityLabel(a.activityLabel)
                .durationMinutes(Math.max(0, (int) Math.round(a.durationSeconds / 60.0)))
                .occurredAt(a.completedAt != null ? a.completedAt : a.startedAt)
                .points(a.points)
                .ruleApplied(a.ruleApplied)
                .dataSource(a.dataSource)
                .validationStatus(status)
                .evidenceUrl(blankToNull(a.photoUrl))
                .adminFlags(parseFlags(review != null ? review.getFlags() : null))
                .adminNote(review != null ? review.getNote() : null)
                .reviewStatus(review != null ? review.getStatus() : null)
                .build();
    }

    private RetoAdminActivityDTOs.ActivityReviewItem toReviewItem(
            RawActivity raw, RetoActivityReview review, String fullName, List<RetoActivityReviewFlag> suggested) {
        return RetoAdminActivityDTOs.ActivityReviewItem.builder()
                .userId(raw.userId)
                .fullName(fullName)
                .source(raw.source)
                .sourceId(raw.id)
                .activityType(raw.activityType)
                .activityLabel(raw.activityLabel)
                .durationMinutes(Math.max(0, (int) Math.round(raw.durationSeconds / 60.0)))
                .occurredAt(raw.completedAt != null ? raw.completedAt : raw.startedAt)
                .evidenceUrl(blankToNull(raw.photoUrl))
                .suggestedFlags(suggested.stream().distinct().toList())
                .adminFlags(parseFlags(review != null ? review.getFlags() : null))
                .adminNote(review != null ? review.getNote() : null)
                .reviewStatus(review != null ? review.getStatus() : null)
                .build();
    }

    private RetoAdminActivityDTOs.HistoryEntry progresoEntry(
            String label, double points, LocalDate weekEnd, String rule) {
        return RetoAdminActivityDTOs.HistoryEntry.builder()
                .kind("PROGRESO")
                .activityType("PROGRESO")
                .activityLabel(label)
                .occurredAt(weekEnd.atTime(23, 59))
                .points(points)
                .ruleApplied(rule)
                .dataSource("Cálculo del reto")
                .validationStatus("PUNTUADA")
                .adminFlags(List.of())
                .build();
    }

    private double[] progresoFuerzaLibre(int semana, double[] ponderados, double[] oneRm, double[] volumen) {
        double libre = mejora(semana, ponderados) * props.getProgresoLibreMax();
        double fuerza = mejora(semana, oneRm) * props.getProgreso1rmMax()
                + mejora(semana, volumen) * props.getProgresoVolumenMax();
        double total = libre + fuerza;
        if (total <= 0 || total <= props.getProgresoMaxSemanal()) {
            return new double[]{fuerza, libre};
        }
        double scale = props.getProgresoMaxSemanal() / total;
        return new double[]{fuerza * scale, libre * scale};
    }

    private double mejora(int semana, double[] serie) {
        int baseline = -1;
        for (int i = 0; i < semana; i++) {
            if (serie[i] > 0) {
                baseline = i;
                break;
            }
        }
        if (baseline < 0 || serie[semana] <= serie[baseline]) return 0;
        double mejoraPct = ((serie[semana] - serie[baseline]) / serie[baseline]) * 100.0;
        return props.fraccionProgreso(mejoraPct);
    }

    private static int indiceSemana(LocalDate inicioReto, LocalDate dia, int semanas) {
        long offset = ChronoUnit.DAYS.between(inicioReto, dia);
        int indice = (int) (offset / 7);
        return Math.max(0, Math.min(indice, semanas - 1));
    }

    private static String dayLabel(LocalDate dia, LocalDate hoy) {
        long diff = ChronoUnit.DAYS.between(dia, hoy);
        if (diff == 0) return "Hoy";
        if (diff == 1) return "Ayer";
        return dia.format(DAY_LABEL);
    }

    private static String labelLibre(FreeActivityType type, String other) {
        if (type == null) return "Actividad libre";
        return switch (type) {
            case CAMINATA -> "Caminata";
            case CAMINADORA -> "Caminadora";
            case RUNNING -> "Running";
            case BICICLETA_ESTATICA -> "Bicicleta";
            case ELIPTICA -> "Elíptica";
            case NATACION -> "Natación";
            case BAILE -> "Baile";
            case YOGA -> "Yoga";
            case FUTBOL -> "Fútbol";
            case BOX -> "Box";
            case CLASE_GRUPAL -> "Clase grupal";
            case OTRA -> (other != null && !other.isBlank()) ? other.trim() : "Otra";
        };
    }

    private static String fullName(User user) {
        if (user == null) return "Usuario";
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "Usuario" : name;
    }

    private static List<RetoActivityReviewFlag> parseFlags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        EnumSet<RetoActivityReviewFlag> set = EnumSet.noneOf(RetoActivityReviewFlag.class);
        for (String part : raw.split(",")) {
            try {
                set.add(RetoActivityReviewFlag.valueOf(part.trim()));
            } catch (IllegalArgumentException ignored) {
                // flag desconocida se ignora
            }
        }
        return new ArrayList<>(set);
    }

    private static String serializeFlags(Collection<RetoActivityReviewFlag> flags) {
        if (flags == null || flags.isEmpty()) return null;
        return flags.stream().map(Enum::name).distinct().sorted().collect(Collectors.joining(","));
    }

    private static String strip(double value) {
        if (Math.abs(value - Math.round(value)) < 1e-9) return String.valueOf(Math.round(value));
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    record SourceKey(RetoActivitySource source, Long sourceId) {}

    static final class RawActivity {
        Long id;
        Long userId;
        RetoActivitySource source;
        String activityType;
        String activityLabel;
        LocalDateTime startedAt;
        LocalDateTime completedAt;
        int durationSeconds;
        Double distanceKm;
        String photoUrl;
        Double progressPercentage;
        Integer completedExercises;
        String dataSource;
        boolean modalidadLibre;
        boolean valid;
        String invalidReason;
        double esfuerzo;
        double minutosPonderados;
        String ruleBase;
        String ruleApplied;
        double points;
        String validationStatus;
        EnumSet<RetoActivityReviewFlag> suggested = EnumSet.noneOf(RetoActivityReviewFlag.class);

        LocalDate dia() {
            LocalDateTime at = completedAt != null ? completedAt : startedAt;
            return at != null ? at.toLocalDate() : LocalDate.now();
        }
    }
}
