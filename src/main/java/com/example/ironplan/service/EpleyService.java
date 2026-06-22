package com.example.ironplan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EpleyService {

    private static final Logger log = LoggerFactory.getLogger(EpleyService.class);

    /**
     * Calcula el 1RM estimado usando la fórmula de Epley.
     * reps >= 30 → NULL (pierde validez).
     */
    public BigDecimal calcularOneRm(BigDecimal pesoKg, int reps) {
        if (pesoKg == null || pesoKg.compareTo(BigDecimal.ZERO) <= 0 || reps <= 0) {
            return null;
        }
        if (reps >= 30) {
            log.warn("Epley: reps >= 30, one_rm no calculado (peso={}, reps={})", pesoKg, reps);
            return null;
        }
        if (reps == 1) {
            return pesoKg.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(
                new BigDecimal(reps).divide(new BigDecimal(30), 4, RoundingMode.HALF_UP)
        );
        return pesoKg.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularOneRm(Double pesoKg, Integer reps) {
        if (pesoKg == null || reps == null) return null;
        return calcularOneRm(BigDecimal.valueOf(pesoKg), reps);
    }

    public BigDecimal calcularVolumenSerie(BigDecimal pesoKg, int reps) {
        if (pesoKg == null || reps <= 0) return BigDecimal.ZERO;
        return pesoKg.multiply(new BigDecimal(reps)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularVolumenSerie(Double pesoKg, Integer reps) {
        if (pesoKg == null || reps == null || reps <= 0) return BigDecimal.ZERO;
        return calcularVolumenSerie(BigDecimal.valueOf(pesoKg), reps);
    }
}
