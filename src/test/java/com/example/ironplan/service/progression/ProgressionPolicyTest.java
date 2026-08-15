package com.example.ironplan.service.progression;

import com.example.ironplan.rest.dto.progress.ProgressionRecommendationDto.RecommendationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProgressionPolicyTest {

    private static final int REPS_MIN = 8;
    private static final int REPS_MAX = 12;
    private static final double INCREMENT_KG = 2.5;

    private final ProgressionPolicy policy = new ProgressionPolicy();

    @Test
    @DisplayName("Sin historial devuelve FIRST_TIME y no sugiere carga")
    void firstTimeWithoutHistory() {
        ProgressionDecision decision = policy.decide(context(List.of()));

        assertEquals(RecommendationType.FIRST_TIME, decision.type());
        assertNull(decision.suggestedWeightKg());
        assertEquals(10, decision.suggestedRepsTarget());
    }

    @Test
    @DisplayName("Cerrar el tope en todas las series con una sola sesión de historial sube la carga")
    void increaseWeightWithSingleSessionAtTop() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 12, 12, 12)
        )));

        assertEquals(RecommendationType.INCREASE_WEIGHT, decision.type());
        assertEquals(52.5, decision.suggestedWeightKg());
        assertEquals(REPS_MIN, decision.suggestedRepsTarget());
    }

    @Test
    @DisplayName("Sube la carga cuando la última sesión cierra el tope y otra ya lo había tocado")
    void increaseWeightWhenTopReachedInTwoSessions() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 12, 12, 12),
                session(50.0, null, 12, 10, 10),
                session(50.0, null, 10, 10, 10)
        )));

        assertEquals(RecommendationType.INCREASE_WEIGHT, decision.type());
        assertEquals(52.5, decision.suggestedWeightKg());
    }

    @Test
    @DisplayName("Un RIR holgado duplica el salto de carga")
    void increaseWeightTwiceWhenEffortWasEasy() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, 2.0, 12, 12, 12)
        )));

        assertEquals(RecommendationType.INCREASE_WEIGHT, decision.type());
        assertEquals(55.0, decision.suggestedWeightKg());
    }

    @Test
    @DisplayName("Tocar el tope en una sola de tres sesiones aún no sube la carga")
    void holdsWeightUntilTopIsConfirmed() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 12, 12, 12),
                session(50.0, null, 10, 10, 10),
                session(50.0, null, 10, 10, 10)
        )));

        assertEquals(RecommendationType.MAINTAIN, decision.type());
        assertEquals(50.0, decision.suggestedWeightKg());
        assertEquals(REPS_MAX, decision.suggestedRepsTarget());
    }

    @Test
    @DisplayName("Varias series bajo el mínimo en la última sesión bajan la carga")
    void decreaseWeightWhenSeveralSetsMissTheMinimum() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 6, 7, 10)
        )));

        assertEquals(RecommendationType.DECREASE_WEIGHT, decision.type());
        assertEquals(47.5, decision.suggestedWeightKg());
        assertEquals(REPS_MIN, decision.suggestedRepsTarget());
    }

    @Test
    @DisplayName("Dos sesiones seguidas sin cerrar el mínimo bajan la carga")
    void decreaseWeightAfterTwoFailedSessions() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 7, 10, 10),
                session(50.0, null, 7, 10, 10)
        )));

        assertEquals(RecommendationType.DECREASE_WEIGHT, decision.type());
        assertEquals(47.5, decision.suggestedWeightKg());
    }

    @Test
    @DisplayName("Una única serie floja no justifica bajar la carga")
    void singleWeakSetDoesNotTriggerDeload() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 7, 10, 10),
                session(50.0, null, 10, 10, 10)
        )));

        assertNotEquals(RecommendationType.DECREASE_WEIGHT, decision.type());
    }

    @Test
    @DisplayName("Dentro del rango sin llegar al tope se suben repeticiones y se mantiene la carga")
    void increaseRepsInsideRange() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(50.0, null, 10, 10, 10)
        )));

        assertEquals(RecommendationType.INCREASE_REPS, decision.type());
        assertEquals(50.0, decision.suggestedWeightKg());
        assertEquals(11, decision.suggestedRepsTarget());
    }

    @Test
    @DisplayName("Sin carga registrada nunca se sugieren kilos")
    void bodyweightExerciseNeverSuggestsWeight() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(null, null, 12, 12, 12)
        )));

        assertEquals(RecommendationType.MAINTAIN, decision.type());
        assertNull(decision.suggestedWeightKg());
        assertEquals(REPS_MAX, decision.suggestedRepsTarget());
    }

    @Test
    @DisplayName("La carga sugerida se redondea a un escalón utilizable")
    void suggestedWeightIsRounded() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(51.6667, null, 12, 12, 12)
        )));

        assertEquals(54.25, decision.suggestedWeightKg());
    }

    @Test
    @DisplayName("Bajar carga nunca deja un peso negativo")
    void deloadNeverGoesBelowZero() {
        ProgressionDecision decision = policy.decide(context(List.of(
                session(1.0, null, 5, 5, 5)
        )));

        assertEquals(RecommendationType.DECREASE_WEIGHT, decision.type());
        assertEquals(0.0, decision.suggestedWeightKg());
    }

    private ProgressionContext context(List<SessionPerformance> sessions) {
        return new ProgressionContext(REPS_MIN, REPS_MAX, INCREMENT_KG, sessions);
    }

    /** Construye una sesión a partir de las repeticiones de cada serie completada. */
    private static SessionPerformance session(Double weightKg, Double avgRir, int... repsPerSet) {
        int setsReachingMax = (int) Arrays.stream(repsPerSet).filter(reps -> reps >= REPS_MAX).count();
        int setsBelowMin = (int) Arrays.stream(repsPerSet).filter(reps -> reps < REPS_MIN).count();
        int avgReps = (int) Math.round(Arrays.stream(repsPerSet).average().orElse(0));
        double volume = weightKg == null ? 0 : Arrays.stream(repsPerSet).map(reps -> (int) (reps * weightKg)).sum();

        return new SessionPerformance(
                LocalDateTime.now(),
                weightKg,
                avgReps,
                repsPerSet.length,
                setsReachingMax,
                setsBelowMin,
                avgRir,
                volume
        );
    }
}
