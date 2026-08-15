package com.example.ironplan.service.progression;

import com.example.ironplan.rest.dto.progress.ProgressionRecommendationDto.RecommendationType;

/**
 * Resultado de aplicar las reglas de progresión.
 *
 * @param suggestedWeightKg null cuando el ejercicio no tiene carga registrada
 */
public record ProgressionDecision(
        RecommendationType type,
        String message,
        Double suggestedWeightKg,
        Integer suggestedRepsTarget
) {}
