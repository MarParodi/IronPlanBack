package com.example.ironplan.repository.projection;

import java.time.LocalDate;

/** Valor agregado de una métrica para un usuario en un día. */
public record MetricaDiariaUsuario(
        Long userId,
        LocalDate fecha,
        Double valor
) {
}
