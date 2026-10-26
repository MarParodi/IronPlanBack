package com.example.ironplan.model;

/** Identifica una actividad concreta (libre o sesión de fuerza) dentro de un reto. */
public record RetoActivityKey(RetoActivitySource source, Long sourceId) {

    public static RetoActivityKey of(RetoActivityReview review) {
        return new RetoActivityKey(review.getSource(), review.getSourceId());
    }
}
