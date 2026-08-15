package com.example.ironplan.service.progression;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Escalón de carga sugerido según el grupo muscular principal: los grupos grandes
 * toleran saltos mayores que el trabajo de aislamiento, donde 2.5 kg puede ser un
 * salto del 20% de la carga.
 */
public final class WeightIncrementResolver {

    public static final double DEFAULT_INCREMENT_KG = 2.5;
    private static final double LOWER_BODY_INCREMENT_KG = 5.0;
    private static final double SMALL_MUSCLE_INCREMENT_KG = 1.25;

    private WeightIncrementResolver() {}

    public static double forPrimaryMuscle(String primaryMuscle) {
        if (primaryMuscle == null || primaryMuscle.isBlank()) {
            return DEFAULT_INCREMENT_KG;
        }

        return switch (withoutAccents(primaryMuscle)) {
            case "piernas", "gluteos", "cuadriceps", "isquiotibiales", "femoral", "gemelos" ->
                    LOWER_BODY_INCREMENT_KG;
            case "brazos", "biceps", "triceps", "antebrazos", "core", "abdominales" ->
                    SMALL_MUSCLE_INCREMENT_KG;
            default -> DEFAULT_INCREMENT_KG;
        };
    }

    private static String withoutAccents(String value) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
