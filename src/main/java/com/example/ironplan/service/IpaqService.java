package com.example.ironplan.service;

import com.example.ironplan.model.CategoriaIpaq;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs.IpaqRequestDto;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs.IpaqResultadoDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class IpaqService {

    private static final BigDecimal MET_CAMINATA = new BigDecimal("3.3");
    private static final BigDecimal MET_MODERADA = new BigDecimal("4.0");
    private static final BigDecimal MET_VIGOROSA = new BigDecimal("8.0");
    private static final BigDecimal LIMITE_MIN_SEMANA = new BigDecimal("960");

    public IpaqResultadoDto calcular(IpaqRequestDto dto) {
        BigDecimal metCaminata = calcularMet(MET_CAMINATA, dto.caminataMinDia(), dto.caminataDiasSemana());
        BigDecimal metModerada = calcularMet(MET_MODERADA, dto.moderadaMinDia(), dto.moderadaDiasSemana());
        BigDecimal metVigorosa = calcularMet(MET_VIGOROSA, dto.vigorosaMinDia(), dto.vigorosaDiasSemana());
        BigDecimal metTotal = metCaminata.add(metModerada).add(metVigorosa);

        boolean esOutlier = detectarOutlier(dto);
        CategoriaIpaq categoria = clasificar(metTotal);

        return new IpaqResultadoDto(metCaminata, metModerada, metVigorosa, metTotal, categoria, esOutlier);
    }

    private BigDecimal calcularMet(BigDecimal factor, Integer minDia, Integer diasSemana) {
        if (minDia == null || diasSemana == null || minDia == 0 || diasSemana == 0) {
            return BigDecimal.ZERO;
        }
        return factor
                .multiply(new BigDecimal(minDia))
                .multiply(new BigDecimal(diasSemana))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean detectarOutlier(IpaqRequestDto dto) {
        return superaLimite(dto.caminataMinDia(), dto.caminataDiasSemana())
                || superaLimite(dto.moderadaMinDia(), dto.moderadaDiasSemana())
                || superaLimite(dto.vigorosaMinDia(), dto.vigorosaDiasSemana());
    }

    private boolean superaLimite(Integer min, Integer dias) {
        if (min == null || dias == null) return false;
        return new BigDecimal(min * dias).compareTo(LIMITE_MIN_SEMANA) > 0;
    }

    private CategoriaIpaq clasificar(BigDecimal metTotal) {
        if (metTotal.compareTo(new BigDecimal("600")) < 0) return CategoriaIpaq.BAJO;
        if (metTotal.compareTo(new BigDecimal("3000")) < 0) return CategoriaIpaq.MODERADO;
        return CategoriaIpaq.ALTO;
    }
}
