package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentExportService {

    private final ExperimentoRetoRepository retoRepo;
    private final ParticipanteRetoRepository participanteRepo;
    private final IpaqRespuestaRepository ipaqRepo;
    private final SusRespuestaRepository susRepo;
    private final SnapshotSemanalUsuarioRepository snapshotRepo;
    private final OrganizationalAccessService accessService;

    @Transactional(readOnly = true)
    public String exportarCsvPrincipal(Long retoId, User admin, boolean incluirOutliers, boolean soloCompletos, boolean incluirInactivos) {
        ExperimentoReto reto = retoRepo.findById(retoId)
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado"));
        accessService.requireManage(reto.getOrganizacion());

        int semanasIntervencion = reto.getSemanasIntervencion() != null ? reto.getSemanasIntervencion() : 8;
        int semInicio = 1;
        int semFin = semanasIntervencion;

        List<String> rows = new ArrayList<>();
        rows.add(String.join(",",
                "participante_id", "categoria", "objetivo", "genero", "edad", "organizacion",
                "ipaq_pre_caminata_dias", "ipaq_pre_caminata_min", "ipaq_pre_mod_dias", "ipaq_pre_mod_min",
                "ipaq_pre_vig_dias", "ipaq_pre_vig_min", "ipaq_pre_met_total", "ipaq_pre_categoria", "ipaq_pre_outlier",
                "ipaq_post_caminata_dias", "ipaq_post_caminata_min", "ipaq_post_mod_dias", "ipaq_post_mod_min",
                "ipaq_post_vig_dias", "ipaq_post_vig_min", "ipaq_post_met_total", "ipaq_post_categoria", "ipaq_post_outlier",
                "delta_met",
                "semanas_intervencion", "semana_num_inicio", "semana_num_fin",
                "volumen_semana_inicio", "one_rm_semana_inicio",
                "volumen_semana_fin", "one_rm_semana_fin",
                "delta_one_rm", "delta_volumen",
                "total_sesiones", "frecuencia_semanal_promedio", "xp_total_acumulado", "posicion_leaderboard_fin",
                "sus_q1", "sus_q2", "sus_q3", "sus_q4", "sus_q5", "sus_q6", "sus_q7", "sus_q8", "sus_q9", "sus_q10", "puntaje_sus",
                "incluir_en_analisis", "tiene_pretest", "tiene_posttest", "tiene_sus"));

        for (ParticipanteReto pr : participanteRepo.findByRetoIdWithUsuario(retoId)) {
            if (!incluirInactivos && !Boolean.TRUE.equals(pr.getActivo())) continue;

            var ipaqPre = ipaqRepo.findByParticipanteRetoIdAndCorte(pr.getId(), IpaqCorte.PRE).orElse(null);
            var ipaqPost = ipaqRepo.findByParticipanteRetoIdAndCorte(pr.getId(), IpaqCorte.POST).orElse(null);

            if (!incluirOutliers) {
                if (ipaqPre != null && Boolean.TRUE.equals(ipaqPre.getEsOutlier())) continue;
                if (ipaqPost != null && Boolean.TRUE.equals(ipaqPost.getEsOutlier())) continue;
            }
            if (soloCompletos && (!Boolean.TRUE.equals(pr.getCompletoPretest()) || !Boolean.TRUE.equals(pr.getCompletoPosttest()))) {
                continue;
            }

            User u = pr.getUsuario();
            int edad = u.getBirthday() != null ? Period.between(u.getBirthday(), LocalDate.now()).getYears() : 0;
            String org = reto.getOrganizacion() != null ? reto.getOrganizacion().getName() : "";

            var sInicio = snapshotRepo.findByRetoIdAndUsuarioIdAndNumeroSemana(retoId, u.getId(), semInicio).orElse(null);
            var sFin = snapshotRepo.findByRetoIdAndUsuarioIdAndNumeroSemana(retoId, u.getId(), semFin).orElse(null);
            var snaps = snapshotRepo.findByRetoIdAndUsuarioIdOrderByNumeroSemanaAsc(retoId, u.getId());

            int totalSesiones = snaps.stream().mapToInt(SnapshotSemanalUsuario::getSesionesCompletadas).sum();
            double freqProm = snaps.isEmpty() ? 0 :
                    snaps.stream().mapToInt(SnapshotSemanalUsuario::getSesionesCompletadas).average().orElse(0);

            BigDecimal deltaMet = null;
            if (ipaqPre != null && ipaqPost != null && ipaqPre.getMetTotalSemana() != null && ipaqPost.getMetTotalSemana() != null) {
                deltaMet = ipaqPost.getMetTotalSemana().subtract(ipaqPre.getMetTotalSemana());
            }

            boolean compararSemanas = semFin > semInicio;
            BigDecimal deltaOneRm = compararSemanas
                    ? delta(sFin != null ? sFin.getOneRmPromedio() : null, sInicio != null ? sInicio.getOneRmPromedio() : null)
                    : null;
            BigDecimal deltaVol = compararSemanas
                    ? delta(sFin != null ? sFin.getVolumenTotalSemana() : null, sInicio != null ? sInicio.getVolumenTotalSemana() : null)
                    : null;

            var sus = susRepo.findByParticipanteRetoId(pr.getId()).orElse(null);

            rows.add(csv(
                    pr.getId(), ParticipanteCategoria.fromUserLevel(u.getLevel()), pr.getObjetivoCodigo(), u.getGender(), edad, org,
                    ipaqField(ipaqPre, "caminataDias"), ipaqField(ipaqPre, "caminataMin"),
                    ipaqField(ipaqPre, "modDias"), ipaqField(ipaqPre, "modMin"),
                    ipaqField(ipaqPre, "vigDias"), ipaqField(ipaqPre, "vigMin"),
                    ipaqPre != null ? ipaqPre.getMetTotalSemana() : "",
                    ipaqPre != null ? ipaqPre.getCategoriaIpaq() : "",
                    ipaqPre != null ? ipaqPre.getEsOutlier() : "",
                    ipaqField(ipaqPost, "caminataDias"), ipaqField(ipaqPost, "caminataMin"),
                    ipaqField(ipaqPost, "modDias"), ipaqField(ipaqPost, "modMin"),
                    ipaqField(ipaqPost, "vigDias"), ipaqField(ipaqPost, "vigMin"),
                    ipaqPost != null ? ipaqPost.getMetTotalSemana() : "",
                    ipaqPost != null ? ipaqPost.getCategoriaIpaq() : "",
                    ipaqPost != null ? ipaqPost.getEsOutlier() : "",
                    deltaMet,
                    semanasIntervencion, semInicio, semFin,
                    snapField(sInicio, "vol"), snapField(sInicio, "orm"),
                    snapField(sFin, "vol"), snapField(sFin, "orm"),
                    deltaOneRm, deltaVol,
                    totalSesiones, round2(freqProm),
                    sFin != null ? sFin.getXpAcumuladoAlFin() : (u.getLifetimeXp() != null ? u.getLifetimeXp() : 0),
                    sFin != null ? sFin.getPosicionLeaderboard() : "",
                    susField(sus, 1), susField(sus, 2), susField(sus, 3), susField(sus, 4), susField(sus, 5),
                    susField(sus, 6), susField(sus, 7), susField(sus, 8), susField(sus, 9), susField(sus, 10),
                    sus != null ? sus.getPuntajeSus() : "",
                    pr.getActivo(), ipaqPre != null, ipaqPost != null, sus != null));
        }

        return rows.stream().collect(Collectors.joining("\n"));
    }

    private Object ipaqField(IpaqRespuesta r, String field) {
        if (r == null) return "";
        return switch (field) {
            case "caminataDias" -> r.getCaminataDiasSemana();
            case "caminataMin" -> r.getCaminataMinDia();
            case "modDias" -> r.getModeradaDiasSemana();
            case "modMin" -> r.getModeradaMinDia();
            case "vigDias" -> r.getVigorosaDiasSemana();
            case "vigMin" -> r.getVigorosaMinDia();
            default -> "";
        };
    }

    private Object snapField(SnapshotSemanalUsuario s, String field) {
        if (s == null) return "";
        return "vol".equals(field) ? s.getVolumenTotalSemana() : s.getOneRmPromedio();
    }

    private Object susField(SusRespuesta s, int q) {
        if (s == null) return "";
        return switch (q) {
            case 1 -> s.getSusQ1(); case 2 -> s.getSusQ2(); case 3 -> s.getSusQ3();
            case 4 -> s.getSusQ4(); case 5 -> s.getSusQ5(); case 6 -> s.getSusQ6();
            case 7 -> s.getSusQ7(); case 8 -> s.getSusQ8(); case 9 -> s.getSusQ9();
            case 10 -> s.getSusQ10(); default -> "";
        };
    }

    private BigDecimal delta(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.subtract(b).setScale(2, RoundingMode.HALF_UP);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String csv(Object... cols) {
        return java.util.Arrays.stream(cols)
                .map(v -> v == null ? "" : escapeCsv(String.valueOf(v)))
                .collect(Collectors.joining(","));
    }

    private String escapeCsv(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
