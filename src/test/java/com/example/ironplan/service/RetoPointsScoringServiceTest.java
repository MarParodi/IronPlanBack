package com.example.ironplan.service;

import com.example.ironplan.config.RetoPointsProperties;
import com.example.ironplan.model.FreeActivityType;
import com.example.ironplan.model.MetricType;
import com.example.ironplan.repository.FreeActivitySessionRepository;
import com.example.ironplan.repository.UserActivityRepository;
import com.example.ironplan.repository.WorkoutSessionRepository;
import com.example.ironplan.repository.WorkoutSetRepository;
import com.example.ironplan.repository.projection.ActividadLibreScoring;
import com.example.ironplan.repository.projection.SesionFuerzaScoring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica que el cálculo reproduzca los ejemplos del documento del reto
 * "Crecemos Más Fuertes, Crecemos Juntos".
 */
class RetoPointsScoringServiceTest {

    private static final Long ANA = 1L;
    private static final Long LUIS = 2L;
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 2);

    private FreeActivitySessionRepository freeActivityRepo;
    private WorkoutSessionRepository workoutSessionRepo;
    private UserActivityRepository activityRepo;
    private WorkoutSetRepository workoutSetRepo;
    private RetoPointsProperties props;
    private RetoPointsScoringService service;

    private final List<ActividadLibreScoring> libres = new ArrayList<>();
    private final List<SesionFuerzaScoring> fuerzas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        freeActivityRepo = mock(FreeActivitySessionRepository.class);
        workoutSessionRepo = mock(WorkoutSessionRepository.class);
        activityRepo = mock(UserActivityRepository.class);
        workoutSetRepo = mock(WorkoutSetRepository.class);
        props = new RetoPointsProperties();
        service = new RetoPointsScoringService(
                freeActivityRepo, workoutSessionRepo, activityRepo, workoutSetRepo, props);

        when(freeActivityRepo.findScoringDataForUsers(anyCollection(), any(), any())).thenReturn(libres);
        when(workoutSessionRepo.findScoringDataForUsers(anyCollection(), any(), any())).thenReturn(fuerzas);
        when(activityRepo.sumDailyMetricByUser(anyCollection(), any(MetricType.class), any(), any()))
                .thenReturn(List.of());
        when(workoutSetRepo.findDailyMaxOneRmForUsers(anyCollection(), any(), any()))
                .thenReturn(List.of());
    }

    // ─── Esfuerzo en actividad libre (§5 del documento) ───────────────────────

    @Test
    @DisplayName("La tabla de esfuerzo de actividad libre coincide con el documento")
    void tablaDeEsfuerzoActividadLibre() {
        assertEquals(9, esfuerzoLibre(FreeActivityType.CAMINATA, 30), 0.5, "Caminata 30 min");
        assertEquals(11, esfuerzoLibre(FreeActivityType.BICICLETA_ESTATICA, 30), 0.5, "Bicicleta 30 min");
        assertEquals(13, esfuerzoLibre(FreeActivityType.RUNNING, 30), 0.5, "Running 30 min");
        assertEquals(18, esfuerzoLibre(FreeActivityType.YOGA, 60), 0.5, "Yoga 60 min");
        assertEquals(25, esfuerzoLibre(FreeActivityType.BOX, 60), 0.5, "Box 60 min");
        assertEquals(25, esfuerzoLibre(FreeActivityType.FUTBOL, 90), 0.5, "Fútbol 90 min, tope de 60");
    }

    @Test
    @DisplayName("La tabla de esfuerzo de fuerza coincide con el documento")
    void tablaDeEsfuerzoFuerza() {
        assertEquals(9, esfuerzoFuerza(30, false), 0.5, "30 min incompleta");
        assertEquals(16, esfuerzoFuerza(30, true), 0.5, "30 min completada");
        assertEquals(20.5, esfuerzoFuerza(45, true), 0.5, "45 min completada");
        assertEquals(25, esfuerzoFuerza(60, true), 0.5, "60 min completada");
        assertEquals(25, esfuerzoFuerza(90, true), 0.5, "90 min completada, tope de 60");
    }

    // ─── Constancia y combinación diaria (§2 y §8) ────────────────────────────

    @Test
    @DisplayName("El ejemplo del documento: fuerza 50 min completada más caminata 30 min suma 41")
    void ejemploDeDiaCombinado() {
        fuerza(ANA, 0, 50, true);
        libre(ANA, 0, FreeActivityType.CAMINATA, 30);

        assertEquals(41, puntaje(ANA), 0.5);
    }

    @Test
    @DisplayName("La constancia se otorga una sola vez por día aunque haya varias actividades")
    void constanciaUnaVezPorDia() {
        libre(ANA, 0, FreeActivityType.CAMINATA, 30);
        libre(ANA, 0, FreeActivityType.CAMINATA, 30);

        // 10 de constancia + 9 + 9, no 20 de constancia.
        assertEquals(28, puntaje(ANA), 0.5);
    }

    @Test
    @DisplayName("Solo puntúan las tres mejores actividades del día")
    void topeDeTresActividadesPorDia() {
        for (int i = 0; i < 5; i++) libre(ANA, 0, FreeActivityType.CAMINATA, 30);

        assertEquals(10 + 9 * 3, puntaje(ANA), 0.5);
    }

    @Test
    @DisplayName("El tope diario limita los puntos competitivos a 50")
    void topeDiario() {
        libre(ANA, 0, FreeActivityType.BOX, 60);
        libre(ANA, 0, FreeActivityType.BOX, 60);
        libre(ANA, 0, FreeActivityType.BOX, 60);

        // 10 + 25.2 * 3 = 85.6, recortado a 50.
        assertEquals(50, puntaje(ANA), 0.001);
    }

    // ─── Validez (§3 y §6) ────────────────────────────────────────────────────

    @Test
    @DisplayName("Las actividades de menos de 10 minutos o sin foto no puntúan")
    void actividadesInvalidas() {
        libre(ANA, 0, FreeActivityType.RUNNING, 9);
        libres.add(new ActividadLibreScoring(
                ANA, INICIO.atTime(10, 0), FreeActivityType.RUNNING, 30 * 60, null));

        assertEquals(0, puntaje(ANA), 0.001);
    }

    @Test
    @DisplayName("Una sesión de fuerza sin trabajo efectivo no puntúa")
    void fuerzaSinTrabajoEfectivo() {
        fuerzas.add(new SesionFuerzaScoring(
                ANA, INICIO.atTime(8, 0), INICIO.atTime(9, 0), 0.0, 0));

        assertEquals(0, puntaje(ANA), 0.001);
    }

    // ─── Bonos de equipo (§14 y §15) ──────────────────────────────────────────

    @Test
    @DisplayName("Con todo el equipo activo 3 días se otorgan el bono de participación y el de nadie se queda atrás")
    void bonosDeEquipoCompletos() {
        for (Long usuario : List.of(ANA, LUIS)) {
            for (int dia = 0; dia < 3; dia++) {
                libre(usuario, dia, FreeActivityType.CAMINATA, 30);
            }
        }

        double individual = 3 * (10 + 9);
        double esperado = 2 * individual + 70 + 25;

        assertEquals(esperado, service.scoreTeam(List.of(ANA, LUIS), INICIO, INICIO.plusDays(6)), 0.5);
    }

    @Test
    @DisplayName("Si un integrante no participa se pierde el bono de nadie se queda atrás")
    void sinBonoNadieSeQuedaAtras() {
        for (int dia = 0; dia < 3; dia++) libre(ANA, dia, FreeActivityType.CAMINATA, 30);

        // Solo 1 de 2 integrantes activo: 50% de participación otorga 20, sin el bono de 25.
        double esperado = 3 * (10 + 9) + 20;

        assertEquals(esperado, service.scoreTeam(List.of(ANA, LUIS), INICIO, INICIO.plusDays(6)), 0.5);
    }

    // ─── Progreso semanal (§10 a §13) ─────────────────────────────────────────

    @Test
    @DisplayName("La primera semana es baseline y no genera bono de progreso")
    void primeraSemanaSinProgreso() {
        libre(ANA, 0, FreeActivityType.CAMINATA, 30);

        assertEquals(19, puntaje(ANA), 0.001);
    }

    @Test
    @DisplayName("Duplicar los minutos ponderados en la segunda semana da el bono máximo de progreso")
    void progresoTopadoEnQuince() {
        libre(ANA, 0, FreeActivityType.CAMINATA, 30);
        libre(ANA, 7, FreeActivityType.CAMINATA, 60);

        // Semana 1: 10 + 9. Semana 2: 10 + 18 + 15 de progreso (mejora del 100%).
        assertEquals(19 + 28 + 15, puntaje(ANA, 13), 0.5);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double esfuerzoLibre(FreeActivityType tipo, int minutos) {
        reset();
        libre(ANA, 0, tipo, minutos);
        // El puntaje del primer día incluye la constancia; la restamos para aislar el esfuerzo.
        return puntaje(ANA) - props.getConstanciaPorDia();
    }

    private double esfuerzoFuerza(int minutos, boolean completada) {
        reset();
        fuerza(ANA, 0, minutos, completada);
        return puntaje(ANA) - props.getConstanciaPorDia();
    }

    private void reset() {
        libres.clear();
        fuerzas.clear();
    }

    private void libre(Long userId, int diaOffset, FreeActivityType tipo, int minutos) {
        libres.add(new ActividadLibreScoring(
                userId,
                INICIO.plusDays(diaOffset).atTime(10, 0),
                tipo,
                minutos * 60,
                "https://foto.test/evidencia.jpg"
        ));
    }

    private void fuerza(Long userId, int diaOffset, int minutos, boolean completada) {
        LocalDateTime inicio = INICIO.plusDays(diaOffset).atTime(8, 0);
        fuerzas.add(new SesionFuerzaScoring(
                userId,
                inicio,
                inicio.plusMinutes(minutos),
                completada ? 100.0 : 60.0,
                3
        ));
    }

    private double puntaje(Long userId) {
        return puntaje(userId, 6);
    }

    private double puntaje(Long userId, int diasDeReto) {
        Collection<Long> roster = List.of(ANA, LUIS);
        Map<Long, Double> scores = service.scoreUsers(roster, INICIO, INICIO.plusDays(diasDeReto));
        return scores.get(userId);
    }
}
