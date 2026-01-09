package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.progress.*;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private final ProgressRepository progressRepo;
    private final ExerciseRepository exerciseRepo;
    private final WorkoutSetRepository workoutSetRepo;

    // Incremento de peso estándar (2.5 kg para la mayoría de ejercicios)
    private static final double WEIGHT_INCREMENT = 2.5;

    public ProgressService(
            ProgressRepository progressRepo,
            ExerciseRepository exerciseRepo,
            WorkoutSetRepository workoutSetRepo
    ) {
        this.progressRepo = progressRepo;
        this.exerciseRepo = exerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
    }

    // ============ RESUMEN GENERAL DE PROGRESO ============

    @Transactional(readOnly = true)
    public ProgressSummaryDto getProgressSummary(User user, int weeksToShow) {
        Long userId = user.getId();

        // Totales
        long totalWorkouts = progressRepo.countCompletedWorkouts(userId);
        long totalSets = progressRepo.countCompletedSets(userId);
        Double totalVolume = progressRepo.sumTotalVolume(userId);
        Long totalMinutes = progressRepo.sumTotalMinutes(userId);

        // Frecuencia y streak
        List<LocalDate> workoutDates = progressRepo.findWorkoutDates(userId);
        int currentStreak = calculateCurrentStreak(workoutDates);
        int longestStreak = calculateLongestStreak(workoutDates);
        double avgWorkoutsPerWeek = calculateAvgWorkoutsPerWeek(workoutDates, weeksToShow);

        // Top ejercicios
        List<Object[]> topExercisesRaw = progressRepo.findTopExercisesByVolume(userId);
        List<ExercisePrDto> topExercises = topExercisesRaw.stream()
                .limit(5)
                .map(row -> {
                    Long exId = (Long) row[0];
                    String exName = (String) row[1];
                    String muscle = (String) row[2];
                    Double topWeight = row[3] != null ? ((Number) row[3]).doubleValue() : null;
                    Double volume = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

                    // Obtener top set para 1RM
                    List<WorkoutSet> topSets = progressRepo.findTopSetForExercise(userId, exId);
                    Double estimated1RM = null;
                    Integer topReps = null;
                    if (!topSets.isEmpty()) {
                        WorkoutSet ts = topSets.get(0);
                        topReps = ts.getReps();
                        if (ts.getWeightKg() != null && ts.getReps() != null && ts.getReps() > 0) {
                            estimated1RM = calculate1RM(ts.getWeightKg(), ts.getReps());
                        }
                    }

                    return new ExercisePrDto(exId, exName, muscle, topWeight, topReps, estimated1RM, volume);
                })
                .toList();

        // Historial semanal
        List<WeeklyStatsDto> weeklyHistory = getWeeklyHistory(user, weeksToShow);

        return new ProgressSummaryDto(
                (int) totalWorkouts,
                (int) totalSets,
                totalVolume != null ? totalVolume : 0.0,
                totalMinutes != null ? totalMinutes.intValue() : 0,
                avgWorkoutsPerWeek,
                currentStreak,
                longestStreak,
                topExercises,
                weeklyHistory
        );
    }

    // ============ HISTORIAL DE UN EJERCICIO ============

    @Transactional(readOnly = true)
    public ExerciseProgressDto getExerciseProgress(User user, Long exerciseId, int sessionsToShow) {
        Long userId = user.getId();

        Exercise exercise = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado: " + exerciseId));

        List<WorkoutSet> allSets = progressRepo.findAllCompletedSetsForExercise(userId, exerciseId);

        if (allSets.isEmpty()) {
            return new ExerciseProgressDto(
                    exerciseId,
                    exercise.getName(),
                    exercise.getPrimaryMuscle(),
                    0, 0.0, null, null,
                    Collections.emptyList()
            );
        }

        // Agrupar por WorkoutExercise (sesión)
        Map<Long, List<WorkoutSet>> setsByWorkoutExercise = allSets.stream()
                .collect(Collectors.groupingBy(ws -> ws.getWorkoutExercise().getId()));

        // Calcular totales
        int totalSessions = setsByWorkoutExercise.size();
        double totalVolume = allSets.stream()
                .filter(ws -> ws.getWeightKg() != null && ws.getReps() != null)
                .mapToDouble(ws -> ws.getWeightKg() * ws.getReps())
                .sum();

        // Top set global
        TopSetDto topSet = null;
        Double estimated1RM = null;
        List<WorkoutSet> topSets = progressRepo.findTopSetForExercise(userId, exerciseId);
        if (!topSets.isEmpty()) {
            WorkoutSet ts = topSets.get(0);
            topSet = new TopSetDto(ts.getWeightKg(), ts.getReps(), ts.getCreatedAt());
            if (ts.getWeightKg() != null && ts.getReps() != null && ts.getReps() > 0) {
                estimated1RM = calculate1RM(ts.getWeightKg(), ts.getReps());
            }
        }

        // Historial por sesión (últimas N)
        List<ExerciseSessionHistoryDto> history = buildSessionHistory(setsByWorkoutExercise, sessionsToShow);

        return new ExerciseProgressDto(
                exerciseId,
                exercise.getName(),
                exercise.getPrimaryMuscle(),
                totalSessions,
                totalVolume,
                topSet,
                estimated1RM,
                history
        );
    }

    // ============ ESTADÍSTICAS SEMANALES ============

    @Transactional(readOnly = true)
    public List<WeeklyStatsDto> getWeeklyHistory(User user, int weeksToShow) {
        Long userId = user.getId();
        List<WeeklyStatsDto> weeks = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int i = 0; i < weeksToShow; i++) {
            LocalDate weekStart = startOfWeek.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            LocalDateTime startDt = weekStart.atStartOfDay();
            LocalDateTime endDt = weekEnd.plusDays(1).atStartOfDay();

            List<WorkoutSet> weekSets = progressRepo.findCompletedSetsInDateRange(userId, startDt, endDt);

            // Agrupar por sesión
            Map<Long, List<WorkoutSet>> setsBySession = weekSets.stream()
                    .collect(Collectors.groupingBy(ws -> ws.getWorkoutExercise().getWorkoutSession().getId()));

            int workoutsCompleted = setsBySession.size();
            int totalSets = weekSets.size();
            double totalVolume = weekSets.stream()
                    .filter(ws -> ws.getWeightKg() != null && ws.getReps() != null)
                    .mapToDouble(ws -> ws.getWeightKg() * ws.getReps())
                    .sum();

            // Calcular minutos
            int totalMinutes = setsBySession.values().stream()
                    .mapToInt(sets -> {
                        if (sets.isEmpty()) return 0;
                        WorkoutSession session = sets.get(0).getWorkoutExercise().getWorkoutSession();
                        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                            return (int) ChronoUnit.MINUTES.between(session.getStartedAt(), session.getCompletedAt());
                        }
                        return 0;
                    })
                    .sum();

            // Daily breakdown
            List<DailyWorkoutDto> dailyBreakdown = buildDailyBreakdown(weekStart, setsBySession);

            weeks.add(new WeeklyStatsDto(
                    weekStart,
                    weekEnd,
                    workoutsCompleted,
                    totalSets,
                    totalVolume,
                    totalMinutes,
                    dailyBreakdown
            ));
        }

        return weeks;
    }

    // ============ RECOMENDACIÓN DE PROGRESIÓN ============

    @Transactional(readOnly = true)
    public ProgressionRecommendationDto getProgressionRecommendation(
            User user,
            Long exerciseId,
            int plannedSets,
            int repsMin,
            int repsMax
    ) {
        Long userId = user.getId();

        Exercise exercise = exerciseRepo.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado: " + exerciseId));

        // Obtener últimos 3 entrenamientos de este ejercicio
        List<WorkoutExercise> recentExercises = progressRepo.findRecentWorkoutExercises(userId, exerciseId);
        
        // Limitar a 3
        recentExercises = recentExercises.stream().limit(3).toList();

        if (recentExercises.isEmpty()) {
            // Primera vez
            return new ProgressionRecommendationDto(
                    exerciseId,
                    exercise.getName(),
                    plannedSets,
                    repsMin,
                    repsMax,
                    Collections.emptyList(),
                    ProgressionRecommendationDto.RecommendationType.FIRST_TIME,
                    "Primera vez con este ejercicio. Empieza con un peso que te permita hacer " + repsMin + "-" + repsMax + " reps con buena técnica.",
                    null,
                    (repsMin + repsMax) / 2
            );
        }

        // Construir historial de rendimiento reciente
        List<RecentPerformanceDto> recentPerformance = buildRecentPerformance(recentExercises, repsMin, repsMax);

        // Aplicar algoritmo de recomendación
        return calculateRecommendation(
                exercise,
                plannedSets,
                repsMin,
                repsMax,
                recentPerformance
        );
    }

    // ============ ALGORITMO DE RECOMENDACIÓN ============

    private ProgressionRecommendationDto calculateRecommendation(
            Exercise exercise,
            int plannedSets,
            int repsMin,
            int repsMax,
            List<RecentPerformanceDto> recentPerformance
    ) {
        // Reglas de progresión:
        // 1. Si alcanzó repsMax en 2 de los últimos 3 entrenamientos -> SUBIR PESO
        // 2. Si no alcanzó repsMin en alguno -> BAJAR PESO
        // 3. Si está en rango pero no alcanza repsMax -> SUBIR REPS (mantener peso)
        // 4. Si todo bien pero no cumple criterio para subir -> MANTENER

        int hitMaxCount = 0;
        int missedMinCount = 0;
        Double lastWeight = null;
        int avgReps = 0;
        int totalReps = 0;
        int count = 0;

        for (RecentPerformanceDto perf : recentPerformance) {
            if (perf.hitMaxReps()) hitMaxCount++;
            if (!perf.hitMinReps()) missedMinCount++;
            if (perf.weightKg() != null) {
                lastWeight = perf.weightKg();
            }
            if (perf.avgReps() != null) {
                totalReps += perf.avgReps();
                count++;
            }
        }

        if (count > 0) {
            avgReps = totalReps / count;
        }

        ProgressionRecommendationDto.RecommendationType type;
        String message;
        Double suggestedWeight = lastWeight;
        Integer suggestedReps = avgReps > 0 ? avgReps : (repsMin + repsMax) / 2;

        // Aplicar reglas
        if (missedMinCount > 0) {
            // BAJAR PESO - No alcanzó el mínimo
            type = ProgressionRecommendationDto.RecommendationType.DECREASE_WEIGHT;
            if (lastWeight != null) {
                suggestedWeight = Math.max(0, lastWeight - WEIGHT_INCREMENT);
            }
            suggestedReps = repsMin;
            message = String.format(
                    "No alcanzaste %d reps en algunas series. Baja el peso a %.1f kg y enfócate en la técnica.",
                    repsMin,
                    suggestedWeight != null ? suggestedWeight : 0
            );
        } else if (hitMaxCount >= 2 && recentPerformance.size() >= 2) {
            // SUBIR PESO - Alcanzó repsMax 2 de 3 veces
            type = ProgressionRecommendationDto.RecommendationType.INCREASE_WEIGHT;
            if (lastWeight != null) {
                suggestedWeight = lastWeight + WEIGHT_INCREMENT;
            }
            suggestedReps = repsMin;
            message = String.format(
                    "¡Excelente progreso! Has alcanzado %d reps consistentemente. Sube a %.1f kg.",
                    repsMax,
                    suggestedWeight != null ? suggestedWeight : 0
            );
        } else if (avgReps < repsMax && avgReps >= repsMin) {
            // SUBIR REPS - Está en rango pero puede mejorar
            type = ProgressionRecommendationDto.RecommendationType.INCREASE_REPS;
            suggestedReps = Math.min(repsMax, avgReps + 1);
            message = String.format(
                    "Buen trabajo. Mantén %.1f kg e intenta alcanzar %d reps en cada serie.",
                    lastWeight != null ? lastWeight : 0,
                    suggestedReps
            );
        } else {
            // MANTENER
            type = ProgressionRecommendationDto.RecommendationType.MAINTAIN;
            message = String.format(
                    "Continúa con %.1f kg x %d-%d reps. Enfócate en la calidad de cada rep.",
                    lastWeight != null ? lastWeight : 0,
                    repsMin,
                    repsMax
            );
        }

        return new ProgressionRecommendationDto(
                exercise.getId(),
                exercise.getName(),
                plannedSets,
                repsMin,
                repsMax,
                recentPerformance,
                type,
                message,
                suggestedWeight,
                suggestedReps
        );
    }

    // ============ HELPERS ============

    /**
     * Calcula 1RM usando la fórmula de Epley: 1RM = peso * (1 + reps/30)
     */
    public static Double calculate1RM(double weight, int reps) {
        if (reps <= 0 || weight <= 0) return null;
        if (reps == 1) return weight;
        return weight * (1 + (double) reps / 30);
    }

    private List<RecentPerformanceDto> buildRecentPerformance(
            List<WorkoutExercise> recentExercises,
            int repsMin,
            int repsMax
    ) {
        List<RecentPerformanceDto> result = new ArrayList<>();

        for (WorkoutExercise we : recentExercises) {
            List<WorkoutSet> sets = workoutSetRepo.findByWorkoutExercise_IdOrderBySetNumberAsc(we.getId());
            
            List<WorkoutSet> completedSets = sets.stream()
                    .filter(WorkoutSet::isCompleted)
                    .filter(ws -> ws.getReps() != null)
                    .toList();

            if (completedSets.isEmpty()) continue;

            double avgWeight = completedSets.stream()
                    .filter(ws -> ws.getWeightKg() != null)
                    .mapToDouble(WorkoutSet::getWeightKg)
                    .average()
                    .orElse(0);

            double avgRepsDouble = completedSets.stream()
                    .mapToInt(WorkoutSet::getReps)
                    .average()
                    .orElse(0);

            int avgReps = (int) Math.round(avgRepsDouble);

            boolean hitMaxReps = completedSets.stream()
                    .anyMatch(ws -> ws.getReps() >= repsMax);

            boolean hitMinReps = completedSets.stream()
                    .allMatch(ws -> ws.getReps() >= repsMin);

            double volume = completedSets.stream()
                    .filter(ws -> ws.getWeightKg() != null)
                    .mapToDouble(ws -> ws.getWeightKg() * ws.getReps())
                    .sum();

            LocalDateTime date = we.getWorkoutSession().getCompletedAt() != null
                    ? we.getWorkoutSession().getCompletedAt()
                    : we.getWorkoutSession().getStartedAt();

            result.add(new RecentPerformanceDto(
                    date,
                    avgWeight > 0 ? avgWeight : null,
                    avgReps,
                    completedSets.size(),
                    hitMaxReps,
                    hitMinReps,
                    volume
            ));
        }

        return result;
    }

    private List<ExerciseSessionHistoryDto> buildSessionHistory(
            Map<Long, List<WorkoutSet>> setsByWorkoutExercise,
            int limit
    ) {
        // Ordenar por fecha (más reciente primero)
        List<Map.Entry<Long, List<WorkoutSet>>> sorted = setsByWorkoutExercise.entrySet().stream()
                .sorted((e1, e2) -> {
                    LocalDateTime d1 = e1.getValue().get(0).getWorkoutExercise().getWorkoutSession().getCompletedAt();
                    LocalDateTime d2 = e2.getValue().get(0).getWorkoutExercise().getWorkoutSession().getCompletedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                })
                .limit(limit)
                .toList();

        List<ExerciseSessionHistoryDto> history = new ArrayList<>();

        for (Map.Entry<Long, List<WorkoutSet>> entry : sorted) {
            List<WorkoutSet> sets = entry.getValue();
            if (sets.isEmpty()) continue;

            WorkoutExercise we = sets.get(0).getWorkoutExercise();
            WorkoutSession session = we.getWorkoutSession();

            double volume = sets.stream()
                    .filter(ws -> ws.getWeightKg() != null && ws.getReps() != null)
                    .mapToDouble(ws -> ws.getWeightKg() * ws.getReps())
                    .sum();

            // Top set de esta sesión
            WorkoutSet topSetWs = sets.stream()
                    .filter(ws -> ws.getWeightKg() != null)
                    .max(Comparator.comparingDouble(WorkoutSet::getWeightKg))
                    .orElse(null);

            TopSetDto topSet = null;
            Double estimated1RM = null;
            if (topSetWs != null) {
                topSet = new TopSetDto(topSetWs.getWeightKg(), topSetWs.getReps(), topSetWs.getCreatedAt());
                if (topSetWs.getReps() != null && topSetWs.getReps() > 0) {
                    estimated1RM = calculate1RM(topSetWs.getWeightKg(), topSetWs.getReps());
                }
            }

            List<SetDetailDto> setDetails = sets.stream()
                    .map(ws -> new SetDetailDto(
                            ws.getSetNumber(),
                            ws.getReps(),
                            ws.getWeightKg(),
                            ws.isCompleted()
                    ))
                    .toList();

            String sessionName = session.getRoutineDetail() != null
                    ? session.getRoutineDetail().getTitle()
                    : "Entrenamiento";

            history.add(new ExerciseSessionHistoryDto(
                    we.getId(),
                    session.getCompletedAt() != null ? session.getCompletedAt() : session.getStartedAt(),
                    sessionName,
                    volume,
                    topSet,
                    estimated1RM,
                    we.getPlannedSets(),
                    we.getCompletedSets(),
                    setDetails
            ));
        }

        return history;
    }

    private List<DailyWorkoutDto> buildDailyBreakdown(LocalDate weekStart, Map<Long, List<WorkoutSet>> setsBySession) {
        List<DailyWorkoutDto> days = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            int dayOfWeek = date.getDayOfWeek().getValue();

            // Buscar si hay workout ese día
            boolean found = false;
            for (Map.Entry<Long, List<WorkoutSet>> entry : setsBySession.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                WorkoutSession session = entry.getValue().get(0).getWorkoutExercise().getWorkoutSession();
                LocalDate sessionDate = session.getCompletedAt() != null
                        ? session.getCompletedAt().toLocalDate()
                        : (session.getStartedAt() != null ? session.getStartedAt().toLocalDate() : null);

                if (date.equals(sessionDate)) {
                    int sets = entry.getValue().size();
                    double volume = entry.getValue().stream()
                            .filter(ws -> ws.getWeightKg() != null && ws.getReps() != null)
                            .mapToDouble(ws -> ws.getWeightKg() * ws.getReps())
                            .sum();
                    int minutes = 0;
                    if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                        minutes = (int) ChronoUnit.MINUTES.between(session.getStartedAt(), session.getCompletedAt());
                    }

                    String sessionName = session.getRoutineDetail() != null
                            ? session.getRoutineDetail().getTitle()
                            : "Entrenamiento";

                    days.add(new DailyWorkoutDto(date, dayOfWeek, true, session.getId(), sessionName, sets, volume, minutes));
                    found = true;
                    break;
                }
            }

            if (!found) {
                days.add(new DailyWorkoutDto(date, dayOfWeek, false, null, null, 0, 0, 0));
            }
        }

        return days;
    }

    private int calculateCurrentStreak(List<LocalDate> workoutDates) {
        if (workoutDates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Si no entrenó hoy ni ayer, streak = 0
        if (!workoutDates.contains(today) && !workoutDates.contains(yesterday)) {
            return 0;
        }

        int streak = 0;
        LocalDate checkDate = workoutDates.contains(today) ? today : yesterday;

        for (LocalDate date : workoutDates) {
            if (date.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (date.isBefore(checkDate)) {
                break;
            }
        }

        return streak;
    }

    private int calculateLongestStreak(List<LocalDate> workoutDates) {
        if (workoutDates.isEmpty()) return 0;

        // Ordenar ascendente para calcular
        List<LocalDate> sorted = workoutDates.stream()
                .sorted()
                .toList();

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < sorted.size(); i++) {
            long daysBetween = ChronoUnit.DAYS.between(sorted.get(i - 1), sorted.get(i));
            if (daysBetween == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    private double calculateAvgWorkoutsPerWeek(List<LocalDate> workoutDates, int weeksToConsider) {
        if (workoutDates.isEmpty()) return 0.0;

        LocalDate cutoff = LocalDate.now().minusWeeks(weeksToConsider);
        long count = workoutDates.stream()
                .filter(d -> !d.isBefore(cutoff))
                .count();

        return (double) count / weeksToConsider;
    }
}
