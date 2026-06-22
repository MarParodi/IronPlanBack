package com.example.ironplan.service;

import com.example.ironplan.model.ClasificacionSus;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs.SusRequestDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SusService {

    public BigDecimal calcularPuntaje(SusRequestDto dto) {
        int sumImpares = (dto.susQ1() - 1) + (dto.susQ3() - 1)
                + (dto.susQ5() - 1) + (dto.susQ7() - 1)
                + (dto.susQ9() - 1);

        int sumPares = (5 - dto.susQ2()) + (5 - dto.susQ4())
                + (5 - dto.susQ6()) + (5 - dto.susQ8())
                + (5 - dto.susQ10());

        return new BigDecimal((sumImpares + sumPares) * 2.5)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public ClasificacionSus clasificar(BigDecimal puntaje) {
        if (puntaje.compareTo(new BigDecimal("50")) < 0) return ClasificacionSus.INACEPTABLE;
        if (puntaje.compareTo(new BigDecimal("70")) < 0) return ClasificacionSus.MARGINAL;
        if (puntaje.compareTo(new BigDecimal("85")) < 0) return ClasificacionSus.ACEPTABLE;
        return ClasificacionSus.EXCELENTE;
    }
}
