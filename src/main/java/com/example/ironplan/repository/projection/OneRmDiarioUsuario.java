package com.example.ironplan.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Mejor 1RM estimado de un usuario en un día. */
public record OneRmDiarioUsuario(
        Long userId,
        LocalDate fecha,
        BigDecimal oneRm
) {
}
