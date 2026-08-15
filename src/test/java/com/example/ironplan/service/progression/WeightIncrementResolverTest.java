package com.example.ironplan.service.progression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightIncrementResolverTest {

    @Test
    @DisplayName("El tren inferior admite saltos de 5 kg")
    void lowerBodyUsesLargeStep() {
        assertEquals(5.0, WeightIncrementResolver.forPrimaryMuscle("Piernas"));
        assertEquals(5.0, WeightIncrementResolver.forPrimaryMuscle("Glúteos"));
    }

    @Test
    @DisplayName("El aislamiento de grupos pequeños usa saltos de 1.25 kg")
    void smallMusclesUseSmallStep() {
        assertEquals(1.25, WeightIncrementResolver.forPrimaryMuscle("Brazos"));
        assertEquals(1.25, WeightIncrementResolver.forPrimaryMuscle("Bíceps"));
        assertEquals(1.25, WeightIncrementResolver.forPrimaryMuscle("Core"));
    }

    @Test
    @DisplayName("Los grupos grandes del tren superior usan el salto estándar")
    void upperBodyUsesDefaultStep() {
        assertEquals(2.5, WeightIncrementResolver.forPrimaryMuscle("Pecho"));
        assertEquals(2.5, WeightIncrementResolver.forPrimaryMuscle("Espalda"));
        assertEquals(2.5, WeightIncrementResolver.forPrimaryMuscle("Hombros"));
    }

    @Test
    @DisplayName("Un grupo desconocido o vacío cae en el salto estándar")
    void unknownMuscleFallsBackToDefault() {
        assertEquals(2.5, WeightIncrementResolver.forPrimaryMuscle(null));
        assertEquals(2.5, WeightIncrementResolver.forPrimaryMuscle("   "));
        assertEquals(2.5, WeightIncrementResolver.forPrimaryMuscle("Trapecio"));
    }
}
