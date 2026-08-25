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

    public enum Modalidad { FUERZA, LIBRE }

    public record UsuarioAnalisis(
            Long userId,
            double puntos,
            double fuerza,
            double libre,
            int diasActivos,
            LocalDateTime lastActivityAt,
            boolean activoEstaSemana,
            int actividadesValidas,
            double puntosHoy,
            double puntosEstaSemana
    ) {
        static UsuarioAnalisis cero(Long userId) {
            return new UsuarioAnalisis(userId, 0, 0, 0, 0, null, false, 0, 0, 0);
        }
    }

    public record Analisis(
            LocalDate inicio,
            LocalDate fin,
            int semanas,
            int semanaActual,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<Long, UsuarioAnalisis> usuarios,
            double bonosDeEquipo,
            double bonosDeEquipoEstaSemana,
            double fuerzaEquipo,
            double libreEquipo
    ) {
        public double puntosEquipo() {
            return usuarios.values().stream().mapToDouble(UsuarioAnalisis::puntos).sum() + bonosDeEquipo;
        }

        public int rosterSize() {
            return usuarios.size();
        }

        public int activosEstaSemana() {
            return (int) usuarios.values().stream().filter(UsuarioAnalisis::activoEstaSemana).count();
        }

        public int totalActividades() {
            return usuarios.values().stream().mapToInt(UsuarioAnalisis::actividadesValidas).sum();
        }

        public double puntosHoy() {
            return usuarios.values().stream().mapToDouble(UsuarioAnalisis::puntosHoy).sum();
        }

        public double puntosEstaSemana() {
            return usuarios.values().stream().mapToDouble(UsuarioAnalisis::puntosEstaSemana).sum()
                    + bonosDeEquipoEstaSemana;
        }

        public double promedioPorIntegrante() {
            return rosterSize() == 0 ? 0 : puntosEquipo() / rosterSize();
        }

        public double porcentajeAporte() {
            if (rosterSize() == 0) return 0;
            long n = usuarios.values().stream().filter(u -> u.puntos() > 0).count();
            return (n * 100.0) / rosterSize();
        }

        public double participacionSemanalPercent() {
            return rosterSize() == 0 ? 0 : (activosEstaSemana() * 100.0) / rosterSize();
        }
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    /** Puntaje individual de cada usuario del roster. Incluye a los que no puntuaron, con 0.0. */
    @Transactional(readOnly = true)
    public Map<Long, Double> scoreUsers(Collection<Long> userIds, LocalDate start, LocalDate end) {
        Map<Long, Double> resultado = new LinkedHashMap<>();
        for (Long id : userIds) resultado.put(id, 0.0);
        analizar(userIds, start, end).usuarios().forEach((id, u) -> resultado.put(id, u.puntos()));
        return resultado;
    }

    /** Puntaje del equipo: suma de sus integrantes más los bonos de participación colectiva. */
    @Transactional(readOnly = true)
    public double scoreTeam(Collection<Long> rosterIds, LocalDate start, LocalDate end) {
        return analizar(rosterIds, start, end).puntosEquipo();
    }

    @Transactional(readOnly = true)
    public Analisis analizar(Collection<Long> userIds, LocalDate start, LocalDate end) {
        return analizar(userIds, start, end, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Analisis analizar(Collection<Long> userIds, LocalDate start, LocalDate end, LocalDate hoy) {
        if (!esRangoValido(userIds, start, end)) {
            return vacio(userIds, start, end, hoy);
        }
        LocalDate referencia = hoy != null ? hoy : LocalDate.now();
        Contexto ctx = cargar(userIds, start, end, referencia);
        return ctx.analisis();
    }

    private boolean esRangoValido(Collection<Long> userIds, LocalDate start, LocalDate end) {
        return userIds != null && !userIds.isEmpty()
                && start != null && end != null && !end.isBefore(start);
    }

    private Analisis vacio(Collection<Long> userIds, LocalDate start, LocalDate end, LocalDate hoy) {
        LocalDate ini = start != null ? start : (hoy != null ? hoy : LocalDate.now());
        LocalDate fin = end != null && start != null && !end.isBefore(start) ? end : ini;
        Map<Long, UsuarioAnalisis> usuarios = new LinkedHashMap<>();
        if (userIds != null) {
            for (Long id : userIds) usuarios.put(id, UsuarioAnalisis.cero(id));
        }
        return new Analisis(ini, fin, 1, 0, ini, fin, usuarios, 0, 0, 0, 0);
    }

    // ─── Carga de datos ───────────────────────────────────────────────────────

    private Contexto cargar(Collection<Long> userIds, LocalDate start, LocalDate end, LocalDate hoy) {
        LocalDateTime desde = start.atStartOfDay();
        LocalDateTime hasta = end.atTime(LocalTime.MAX);
        int semanas = (int) (ChronoUnit.DAYS.between(start, end) / 7) + 1;

        Map<Long, List<Actividad>> actividades = new LinkedHashMap<>();
        for (Long id : userIds) actividades.put(id, new ArrayList<>());

        for (var a : freeActivityRepo.findScoringDataForUsers(userIds, desde, hasta)) {
            Actividad valida = validarActividadLibre(a);
            List<Actividad> dest = valida == null ? null : actividades.get(a.userId());
            if (dest != null) dest.add(valida);
        }
        for (var s : workoutSessionRepo.findScoringDataForUsers(userIds, desde, hasta)) {
            Actividad valida = validarSesionFuerza(s);
            List<Actividad> dest = valida == null ? null : actividades.get(s.userId());
            if (dest != null) dest.add(valida);
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

        return new Contexto(start, end, semanas, hoy, actividades, volumenSemanal, oneRmSemanal);
    }

    private Actividad validarActividadLibre(ActividadLibreScoring a) {
        if (a.durationSeconds() == null || a.completedAt() == null) return null;
        double minutos = a.durationSeconds() / 60.0;
        if (minutos < props.getMinutosMinimos()) return null;
        if (props.isRequiereFotoActividadLibre() && (a.photoUrl() == null || a.photoUrl().isBlank())) return null;

        double puntuables = Math.min(minutos, props.getMinutosMaxPuntuables());
        double intensidad = props.intensidadDe(a.activityType());
        double esfuerzo = bloques(puntuables) * props.getPuntosPorBloque() * intensidad;

        return new Actividad(a.completedAt(), esfuerzo, puntuables * intensidad, Modalidad.LIBRE);
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

        return new Actividad(s.completedAt(), esfuerzo, 0.0, Modalidad.FUERZA);
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

    private record Actividad(
            LocalDateTime completedAt,
            double esfuerzo,
            double minutosPonderados,
            Modalidad modalidad
    ) {
        LocalDate dia() {
            return completedAt.toLocalDate();
        }
    }

    private final class Contexto {
        private final LocalDate inicio;
        private final LocalDate fin;
        private final int semanas;
        private final int semanaActual;
        private final LocalDate weekStart;
        private final LocalDate weekEnd;
        private final LocalDate hoy;
        private final Map<Long, List<Actividad>> actividades;
        private final Map<Long, double[]> volumenSemanal;
        private final Map<Long, double[]> oneRmSemanal;

        private final Map<Long, UsuarioAnalisis> usuarios = new LinkedHashMap<>();

        /** userId -> semana -> días distintos con actividad válida. */
        private final Map<Long, List<Set<LocalDate>>> diasActivosPorSemana = new HashMap<>();

        private Contexto(
                LocalDate inicio,
                LocalDate fin,
                int semanas,
                LocalDate hoy,
                Map<Long, List<Actividad>> actividades,
                Map<Long, double[]> volumenSemanal,
                Map<Long, double[]> oneRmSemanal
        ) {
            this.inicio = inicio;
            this.fin = fin;
            this.semanas = semanas;
            this.hoy = hoy;
            this.semanaActual = indiceSemana(inicio, fin, semanas);
            this.weekStart = inicio.plusDays((long) semanaActual * 7);
            LocalDate finSemana = weekStart.plusDays(6);
            this.weekEnd = finSemana.isAfter(fin) ? fin : finSemana;
            this.actividades = actividades;
            this.volumenSemanal = volumenSemanal;
            this.oneRmSemanal = oneRmSemanal;
            actividades.forEach((userId, lista) -> usuarios.put(userId, puntajeUsuario(userId, lista)));
        }

        Analisis analisis() {
            double fuerza = usuarios.values().stream().mapToDouble(UsuarioAnalisis::fuerza).sum();
            double libre = usuarios.values().stream().mapToDouble(UsuarioAnalisis::libre).sum();
            double[] bonos = bonosPorSemana(usuarios.size());
            double totalBonos = 0;
            for (double b : bonos) totalBonos += b;
            double bonosSemana = semanaActual >= 0 && semanaActual < bonos.length ? bonos[semanaActual] : 0;
            return new Analisis(
                    inicio, fin, semanas, semanaActual, weekStart, weekEnd,
                    usuarios, totalBonos, bonosSemana, fuerza, libre
            );
        }

        private UsuarioAnalisis puntajeUsuario(Long userId, List<Actividad> lista) {
            double[] puntosSemana = new double[semanas];
            double[] ponderadosSemana = new double[semanas];
            List<Set<LocalDate>> diasActivos = nuevaListaDeSemanas();

            double fuerza = 0;
            double libre = 0;
            double puntosHoy = 0;
            LocalDateTime lastActivityAt = null;

            Map<LocalDate, List<Actividad>> porDia = new HashMap<>();
            for (Actividad a : lista) {
                porDia.computeIfAbsent(a.dia(), k -> new ArrayList<>()).add(a);
                if (lastActivityAt == null || a.completedAt().isAfter(lastActivityAt)) {
                    lastActivityAt = a.completedAt();
                }
            }

            for (var entry : porDia.entrySet()) {
                LocalDate dia = entry.getKey();
                List<Actividad> delDia = entry.getValue();
                List<Actividad> puntuables = delDia.stream()
                        .sorted(Comparator.comparingDouble(Actividad::esfuerzo).reversed())
                        .limit(props.getMaxActividadesDia())
                        .toList();

                double rawFuerza = 0;
                double rawLibre = 0;
                double ponderados = 0;
                for (Actividad a : puntuables) {
                    if (a.modalidad() == Modalidad.FUERZA) rawFuerza += a.esfuerzo();
                    else rawLibre += a.esfuerzo();
                    ponderados += a.minutosPonderados();
                }

                double constancia = props.getConstanciaPorDia();
                double uncapped = constancia + rawFuerza + rawLibre;
                double capped = Math.min(props.getTopeDiario(), uncapped);
                double scale = uncapped <= 0 ? 0 : capped / uncapped;
                double f = rawFuerza * scale;
                double l = rawLibre * scale;
                double c = constancia * scale;
                if (rawFuerza >= rawLibre) f += c;
                else l += c;

                fuerza += f;
                libre += l;

                int semana = indiceSemana(inicio, dia, semanas);
                puntosSemana[semana] += capped;
                ponderadosSemana[semana] += ponderados;
                diasActivos.get(semana).add(dia);
                if (dia.equals(hoy)) puntosHoy += capped;
            }

            diasActivosPorSemana.put(userId, diasActivos);

            double[] volumen = volumenSemanal.getOrDefault(userId, new double[semanas]);
            double[] oneRm = oneRmSemanal.getOrDefault(userId, new double[semanas]);

            double total = 0;
            for (int semana = 0; semana < semanas; semana++) {
                double[] progreso = progresoFuerzaLibre(semana, ponderadosSemana, oneRm, volumen);
                fuerza += progreso[0];
                libre += progreso[1];
                puntosSemana[semana] += progreso[0] + progreso[1];
                total += puntosSemana[semana];
            }

            int dias = diasActivos.stream().mapToInt(Set::size).sum();
            boolean activoEstaSemana = diasActivos.get(semanaActual).size() > 0;
            double puntosEstaSemana = puntosSemana[semanaActual];

            return new UsuarioAnalisis(
                    userId, total, fuerza, libre, dias, lastActivityAt,
                    activoEstaSemana, lista.size(), puntosHoy, puntosEstaSemana
            );
        }

        /**
         * Compara cada semana contra la primera con datos de esa modalidad, no contra la
         * semana anterior, para no exigir mejorar indefinidamente ni castigar a quien
         * empieza tarde.
         *
         * @return [fuerza, libre] ya recortados al tope semanal de progreso
         */
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

        private double[] bonosPorSemana(int tamanioRoster) {
            double[] porSemana = new double[semanas];
            if (tamanioRoster <= 0) return porSemana;

            for (int semana = 0; semana < semanas; semana++) {
                int constantes = 0;
                int conAlgunaActividad = 0;
                for (List<Set<LocalDate>> porSemanaDias : diasActivosPorSemana.values()) {
                    int dias = porSemanaDias.get(semana).size();
                    if (dias >= props.getDiasActivosParaBonoEquipo()) constantes++;
                    if (dias > 0) conAlgunaActividad++;
                }

                porSemana[semana] += props.puntosParticipacion((constantes * 100.0) / tamanioRoster);
                if (conAlgunaActividad >= tamanioRoster) {
                    porSemana[semana] += props.getBonoNadieSeQuedaAtras();
                }
            }
            return porSemana;
        }

        private List<Set<LocalDate>> nuevaListaDeSemanas() {
            List<Set<LocalDate>> lista = new ArrayList<>(semanas);
            for (int i = 0; i < semanas; i++) lista.add(new HashSet<>());
            return lista;
        }
    }
}
