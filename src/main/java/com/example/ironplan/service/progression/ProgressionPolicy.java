package com.example.ironplan.service.progression;

import com.example.ironplan.rest.dto.progress.ProgressionRecommendationDto.RecommendationType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Reglas de doble progresión: primero se sube en repeticiones dentro del rango
 * planificado y solo cuando el rango se domina de forma consistente se sube la carga.
 *
 * No consulta la base de datos ni depende del contexto de Spring: recibe el historial
 * ya agregado y devuelve una decisión.
 */
@Service
public class ProgressionPolicy {

    /** Las cargas se redondean a este escalón para no arrastrar decimales de los promedios. */
    private static final double WEIGHT_ROUNDING_STEP_KG = 0.25;

    /** RIR medio a partir del cual la serie se considera holgada y admite un salto doble. */
    private static final int EASY_EFFORT_RIR = 2;

    /** Sesiones que deben alcanzar el tope del rango antes de subir carga. */
    private static final int SESSIONS_AT_MAX_REQUIRED = 2;

    public ProgressionDecision decide(ProgressionContext context) {
        List<SessionPerformance> sessions = context.recentSessions();
        if (sessions.isEmpty()) {
            return firstTime(context);
        }

        SessionPerformance last = sessions.get(0);
        Double baseWeight = last.weightKg();

        if (needsDeload(sessions)) {
            return decreaseWeight(context, baseWeight);
        }

        if (readyForMoreWeight(sessions)) {
            return baseWeight == null
                    ? topOfRangeWithoutLoad(context)
                    : increaseWeight(context, baseWeight, last);
        }

        if (last.avgReps() >= context.repsMin() && last.avgReps() < context.repsMax()) {
            return increaseReps(context, baseWeight, last);
        }

        return maintain(context, baseWeight, last);
    }

    /**
     * Bajar carga exige evidencia real de exceso: varias series fallidas en la última
     * sesión o dos sesiones seguidas sin cerrar el mínimo. Una sola serie floja puede ser
     * un mal día y no justifica un deload.
     */
    private boolean needsDeload(List<SessionPerformance> sessions) {
        SessionPerformance last = sessions.get(0);
        if (last.setsBelowMin() >= 2) {
            return true;
        }
        return sessions.size() >= 2
                && !last.metMinimumInAllSets()
                && !sessions.get(1).metMinimumInAllSets();
    }

    /**
     * Subir carga exige cerrar el tope del rango en todas las series de la última sesión,
     * y además haberlo tocado en dos sesiones cuando hay historial suficiente.
     */
    private boolean readyForMoreWeight(List<SessionPerformance> sessions) {
        if (!sessions.get(0).allSetsReachedMax()) {
            return false;
        }
        if (sessions.size() == 1) {
            return true;
        }
        long sessionsAtMax = sessions.stream().filter(SessionPerformance::reachedMax).count();
        return sessionsAtMax >= SESSIONS_AT_MAX_REQUIRED;
    }

    private ProgressionDecision firstTime(ProgressionContext context) {
        return new ProgressionDecision(
                RecommendationType.FIRST_TIME,
                format("Todavía no hay series registradas de este ejercicio. Empieza con un peso que te permita hacer %d-%d reps con buena técnica.",
                        context.repsMin(), context.repsMax()),
                null,
                context.repsMidpoint()
        );
    }

    private ProgressionDecision increaseWeight(ProgressionContext context, double baseWeight, SessionPerformance last) {
        boolean easyEffort = last.avgRir() != null && last.avgRir() >= EASY_EFFORT_RIR;
        double increment = easyEffort ? context.weightIncrementKg() * 2 : context.weightIncrementKg();
        double suggested = roundToStep(baseWeight + increment);

        String message = easyEffort
                ? format("Cerraste todas las series en %d reps y te sobraron %.0f reps. Salta a %.1f kg.",
                        context.repsMax(), last.avgRir(), suggested)
                : format("Cerraste todas las series en %d reps. Sube a %.1f kg.",
                        context.repsMax(), suggested);

        return new ProgressionDecision(RecommendationType.INCREASE_WEIGHT, message, suggested, context.repsMin());
    }

    private ProgressionDecision decreaseWeight(ProgressionContext context, Double baseWeight) {
        if (baseWeight == null) {
            return new ProgressionDecision(
                    RecommendationType.DECREASE_WEIGHT,
                    format("Te quedaste por debajo de %d reps en varias series. Reduce el rango de movimiento o usa asistencia y prioriza la técnica.",
                            context.repsMin()),
                    null,
                    context.repsMin()
            );
        }

        double suggested = roundToStep(Math.max(0, baseWeight - context.weightIncrementKg()));
        return new ProgressionDecision(
                RecommendationType.DECREASE_WEIGHT,
                format("Te quedaste por debajo de %d reps en varias series. Baja a %.1f kg y prioriza la técnica.",
                        context.repsMin(), suggested),
                suggested,
                context.repsMin()
        );
    }

    private ProgressionDecision increaseReps(ProgressionContext context, Double baseWeight, SessionPerformance last) {
        int target = Math.min(context.repsMax(), last.avgReps() + 1);

        String message = baseWeight == null
                ? format("Vas bien. Apunta a %d reps en cada serie antes de añadir dificultad.", target)
                : format("Vas bien. Mantén %.1f kg y apunta a %d reps en cada serie.", baseWeight, target);

        return new ProgressionDecision(RecommendationType.INCREASE_REPS, message, baseWeight, target);
    }

    /** Tope del rango en un ejercicio sin carga registrada: no hay kilos que sugerir. */
    private ProgressionDecision topOfRangeWithoutLoad(ProgressionContext context) {
        return new ProgressionDecision(
                RecommendationType.MAINTAIN,
                format("Llegaste al tope de %d reps sin peso registrado. Añade lastre o suma una serie para seguir progresando.",
                        context.repsMax()),
                null,
                context.repsMax()
        );
    }

    private ProgressionDecision maintain(ProgressionContext context, Double baseWeight, SessionPerformance last) {
        Integer target = last.avgReps() > 0 ? last.avgReps() : context.repsMidpoint();

        if (last.allSetsReachedMax()) {
            String message = baseWeight == null
                    ? format("Tocaste el tope de %d reps. Repítelo una sesión más para confirmar antes de subir dificultad.",
                            context.repsMax())
                    : format("Tocaste el tope de %d reps con %.1f kg. Repítelo una sesión más para confirmar antes de subir.",
                            context.repsMax(), baseWeight);
            return new ProgressionDecision(RecommendationType.MAINTAIN, message, baseWeight, context.repsMax());
        }

        String message = baseWeight == null
                ? format("Sigue en el rango de %d-%d reps y cuida la ejecución.", context.repsMin(), context.repsMax())
                : format("Sigue con %.1f kg x %d-%d reps y cuida la ejecución.",
                        baseWeight, context.repsMin(), context.repsMax());

        return new ProgressionDecision(RecommendationType.MAINTAIN, message, baseWeight, target);
    }

    private static double roundToStep(double weightKg) {
        return Math.round(weightKg / WEIGHT_ROUNDING_STEP_KG) * WEIGHT_ROUNDING_STEP_KG;
    }

    private static String format(String template, Object... args) {
        return String.format(Locale.ROOT, template, args);
    }
}
