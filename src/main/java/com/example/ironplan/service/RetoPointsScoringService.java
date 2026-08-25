package com.example.ironplan.service;

import com.example.ironplan.config.RetoPointsProperties;
import com.example.ironplan.model.MetricType;
import com.example.ironplan.repository.FreeActivitySessionRepository;
import com.example.ironplan.repository.UserActivityRepository;
import com.example.ironplan.repository.WorkoutSessionRepository;
import com.example.ironplan.repository.WorkoutSetRepository;
import com.example.ironplan.repository.projection.ActividadLibreScoring;
import com.example.ironplan.repository.projection.SesionFuerzaScoring;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcula la métrica {@link MetricType#TEAM_POINTS}: un puntaje compuesto de
 * constancia, esfuerzo, progreso personal y bonos de participación colectiva.
 *
 * <p>A diferencia del resto de métricas, no se acumula en {@code user_activities}:
 * los topes diarios, el progreso semanal y los bonos de equipo dependen de agregados
 * que no pueden calcularse de forma incremental al registrar cada actividad.
 *
 * <p>Todo el roster se resuelve con cuatro consultas, independientemente de su tamaño.
 */
@Service
@RequiredArgsConstructor
public class RetoPointsScoringService {

    private final FreeActivitySessionRepository freeActivityRepo;
    private final WorkoutSessionRepository workoutSessionRepo;
    private final UserActivityRepository activityRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final RetoPointsProperties props;

    // ─── API pública ──────────────────────────────────────────────────────────

    /** Puntaje individual de cada usuario del roster. Incluye a los que no puntuaron, con 0.0. */
    @Transactional(readOnly = true)
    public Map<Long, Double> scoreUsers(Collection<Long> userIds, LocalDate start, LocalDate end) {
        Map<Long, Double> resultado = new LinkedHashMap<>();
        for (Long id : userIds) resultado.put(id, 0.0);
        if (!esRangoValido(userIds, start, end)) return resultado;

        Contexto ctx = cargar(userIds, start, end);
        ctx.puntajePorUsuario().forEach(resultado::put);
        return resultado;
    }

    /** Puntaje del equipo: suma de sus integrantes más los bonos de participación colectiva. */
    @Transactional(readOnly = true)
    public double scoreTeam(Collection<Long> rosterIds, LocalDate start, LocalDate end) {
        if (!esRangoValido(rosterIds, start, end)) return 0.0;

        Contexto ctx = cargar(rosterIds, start, end);
        double total = ctx.puntajePorUsuario().values().stream().mapToDouble(Double::doubleValue).sum();
        return total + ctx.bonosDeEquipo(rosterIds.size());
    }

    private boolean esRangoValido(Collection<Long> userIds, LocalDate start, LocalDate end) {
        return userIds != null && !userIds.isEmpty()
                && start != null && end != null && !end.isBefore(start);
    }

    // ─── Carga de datos ───────────────────────────────────────────────────────

    private Contexto cargar(Collection<Long> userIds, LocalDate start, LocalDate end) {
        LocalDateTime desde = start.atStartOfDay();
        LocalDateTime hasta = end.atTime(LocalTime.MAX);
        int semanas = (int) (ChronoUnit.DAYS.between(start, end) / 7) + 1;

        Map<Long, List<Actividad>> actividades = new HashMap<>();
        for (Long id : userIds) actividades.put(id, new ArrayList<>());

        for (var a : freeActivityRepo.findScoringDataForUsers(userIds, desde, hasta)) {
            Actividad valida = validarActividadLibre(a);
            if (valida != null) actividades.get(a.userId()).add(valida);
        }
        for (var s : workoutSessionRepo.findScoringDataForUsers(userIds, desde, hasta)) {
            Actividad valida = validarSesionFuerza(s);
            if (valida != null) actividades.get(s.userId()).add(valida);
        }

        Map<Long, double[]> volumenSemanal = new HashMap<>();
        for (var v : activityRepo.sumDailyMetricByUser(userIds, MetricType.VOLUME_TOTAL, start, end)) {
            double[] semanal = volumenSemanal.computeIfAbsent(v.userId(), k -> new double[semanas]);
            semanal[indiceSemana(start, v.fecha(), semanas)] += v.valor() != null ? v.valor() : 0.0;
        }

        Map<Long, double[]> oneRmSemanal = new HashMap<>();
        for (var r : workoutSetRepo.findDailyMaxOneRmForUsers(userIds, start, end)) {
            if (r.oneRm() == null) continue;
            double[] semanal = oneRmSemanal.computeIfAbsent(r.userId(), k -> new double[semanas]);
            int i = indiceSemana(start, r.fecha(), semanas);
            semanal[i] = Math.max(semanal[i], r.oneRm().doubleValue());
        }

        return new Contexto(start, semanas, actividades, volumenSemanal, oneRmSemanal);
    }

    private Actividad validarActividadLibre(ActividadLibreScoring a) {
        if (a.durationSeconds() == null || a.completedAt() == null) return null;
        double minutos = a.durationSeconds() / 60.0;
        if (minutos < props.getMinutosMinimos()) return null;
        if (props.isRequiereFotoActividadLibre() && (a.photoUrl() == null || a.photoUrl().isBlank())) return null;

        double puntuables = Math.min(minutos, props.getMinutosMaxPuntuables());
        double intensidad = props.intensidadDe(a.activityType());
        double esfuerzo = bloques(puntuables) * props.getPuntosPorBloque() * intensidad;

        return new Actividad(a.completedAt().toLocalDate(), esfuerzo, puntuables * intensidad);
    }

    private Actividad validarSesionFuerza(SesionFuerzaScoring s) {
        if (s.startedAt() == null || s.completedAt() == null) return null;
        if (s.completedExercises() == null || s.completedExercises() < props.getMinEjerciciosCompletados()) return null;

        double minutos = Duration.between(s.startedAt(), s.completedAt()).toSeconds() / 60.0;
        if (minutos < props.getMinutosMinimos()) return null;

        double puntuables = Math.min(minutos, props.getMinutosMaxPuntuables());
        double esfuerzo = bloques(puntuables) * props.getPuntosPorBloque();
        if (s.progressPercentage() != null && s.progressPercentage() >= 100.0) {
            esfuerzo += props.getBonoRutinaCompleta();
        }

        // El progreso en fuerza se mide con 1RM y volumen, no con minutos ponderados.
        return new Actividad(s.completedAt().toLocalDate(), esfuerzo, 0.0);
    }

    private double bloques(double minutosPuntuables) {
        return minutosPuntuables / props.getMinutosPorBloque();
    }

    private static int indiceSemana(LocalDate inicioReto, LocalDate dia, int semanas) {
        long offset = ChronoUnit.DAYS.between(inicioReto, dia);
        int indice = (int) (offset / 7);
        return Math.max(0, Math.min(indice, semanas - 1));
    }

    // ─── Cálculo ──────────────────────────────────────────────────────────────

    private record Actividad(LocalDate dia, double esfuerzo, double minutosPonderados) {
    }

    private final class Contexto {
        private final LocalDate inicio;
        private final int semanas;
        private final Map<Long, List<Actividad>> actividades;
        private final Map<Long, double[]> volumenSemanal;
        private final Map<Long, double[]> oneRmSemanal;

        private final Map<Long, Double> puntajes = new LinkedHashMap<>();

        /** userId -> semana -> días distintos con actividad válida. */
        private final Map<Long, List<Set<LocalDate>>> diasActivosPorSemana = new HashMap<>();

        private Contexto(
                LocalDate inicio,
                int semanas,
                Map<Long, List<Actividad>> actividades,
                Map<Long, double[]> volumenSemanal,
                Map<Long, double[]> oneRmSemanal
        ) {
            this.inicio = inicio;
            this.semanas = semanas;
            this.actividades = actividades;
            this.volumenSemanal = volumenSemanal;
            this.oneRmSemanal = oneRmSemanal;
            actividades.forEach((userId, lista) -> puntajes.put(userId, puntajeUsuario(userId, lista)));
        }

        Map<Long, Double> puntajePorUsuario() {
            return puntajes;
        }

        private double puntajeUsuario(Long userId, List<Actividad> lista) {
            double[] puntosSemana = new double[semanas];
            double[] ponderadosSemana = new double[semanas];
            List<Set<LocalDate>> diasActivos = nuevaListaDeSemanas();

            // Puntos diarios: constancia una vez al día + esfuerzo de las mejores actividades.
            Map<LocalDate, List<Actividad>> porDia = new HashMap<>();
            for (Actividad a : lista) porDia.computeIfAbsent(a.dia(), k -> new ArrayList<>()).add(a);

            porDia.forEach((dia, delDia) -> {
                List<Actividad> puntuables = delDia.stream()
                        .sorted(Comparator.comparingDouble(Actividad::esfuerzo).reversed())
                        .limit(props.getMaxActividadesDia())
                        .toList();

                double esfuerzo = puntuables.stream().mapToDouble(Actividad::esfuerzo).sum();
                double ponderados = puntuables.stream().mapToDouble(Actividad::minutosPonderados).sum();

                int semana = indiceSemana(inicio, dia, semanas);
                puntosSemana[semana] += Math.min(props.getTopeDiario(), props.getConstanciaPorDia() + esfuerzo);
                ponderadosSemana[semana] += ponderados;
                diasActivos.get(semana).add(dia);
            });

            diasActivosPorSemana.put(userId, diasActivos);

            double[] volumen = volumenSemanal.getOrDefault(userId, new double[semanas]);
            double[] oneRm = oneRmSemanal.getOrDefault(userId, new double[semanas]);

            double total = 0;
            for (int semana = 0; semana < semanas; semana++) {
                total += puntosSemana[semana] + bonoProgreso(semana, ponderadosSemana, oneRm, volumen);
            }
            return total;
        }

        /**
         * Compara cada semana contra la primera con datos de esa modalidad, no contra la
         * semana anterior, para no exigir mejorar indefinidamente ni castigar a quien
         * empieza tarde.
         */
        private double bonoProgreso(int semana, double[] ponderados, double[] oneRm, double[] volumen) {
            double bono = mejora(semana, ponderados) * props.getProgresoLibreMax()
                    + mejora(semana, oneRm) * props.getProgreso1rmMax()
                    + mejora(semana, volumen) * props.getProgresoVolumenMax();
            return Math.min(props.getProgresoMaxSemanal(), bono);
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

        double bonosDeEquipo(int tamanioRoster) {
            if (tamanioRoster <= 0) return 0;

            double total = 0;
            for (int semana = 0; semana < semanas; semana++) {
                int constantes = 0;
                int conAlgunaActividad = 0;
                for (List<Set<LocalDate>> porSemana : diasActivosPorSemana.values()) {
                    int dias = porSemana.get(semana).size();
                    if (dias >= props.getDiasActivosParaBonoEquipo()) constantes++;
                    if (dias > 0) conAlgunaActividad++;
                }

                total += props.puntosParticipacion((constantes * 100.0) / tamanioRoster);
                if (conAlgunaActividad >= tamanioRoster) {
                    total += props.getBonoNadieSeQuedaAtras();
                }
            }
            return total;
        }

        private List<Set<LocalDate>> nuevaListaDeSemanas() {
            List<Set<LocalDate>> lista = new ArrayList<>(semanas);
            for (int i = 0; i < semanas; i++) lista.add(new HashSet<>());
            return lista;
        }
    }
}
