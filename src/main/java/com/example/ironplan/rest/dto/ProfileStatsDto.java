// ProfileStatsDto.java
package com.example.ironplan.rest.dto;

public record ProfileStatsDto(
        long totalWorkouts,        // entrenamientos completados
        long totalRoutinesOwned,   // rutinas creadas por el usuario
        long totalXpActions        // cantidad de eventos de XP
) {}
