package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.experimento.ExperimentoDTOs;
import com.example.ironplan.rest.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExperimentoRetoService {

    private static final Set<String> OBJETIVOS_VALIDOS = Set.of(
            "OBJ-1", "OBJ-2", "OBJ-3", "OBJ-4", "OBJ-5", "OBJ-6", "OBJ-7"
    );

    private final ExperimentoRetoRepository retoRepo;
    private final ParticipanteRetoRepository participanteRepo;
    private final ConsentimientoInformadoRepository consentimientoRepo;
    private final IpaqRespuestaRepository ipaqRepo;
    private final SusRespuestaRepository susRepo;
    private final IpaqService ipaqService;
    private final SusService susService;
    private final OrganizationalAccessService accessService;
    private final OrganizationalGroupRepository groupRepo;
    private final CompetitionRepository competitionRepo;
    private final CompetitionService competitionService;
    private final SnapshotSemanalUsuarioRepository snapshotRepo;

    @Transactional(readOnly = true)
    public List<ExperimentoDTOs.RetoResumenResponse> listRetosActivosParaUsuario(User user) {
        return retoRepo.findByEstado(ExperimentoRetoEstado.ACTIVO).stream()
                .filter(r -> accessService.canView(user, r.getOrganizacion()))
                .map(this::toResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExperimentoDTOs.RetoResumenResponse> listRetosParaAdmin(User user) {
        return retoRepo.findAll().stream()
                .filter(r -> accessService.canManage(user, r.getOrganizacion()))
                .map(this::toResumen)
                .toList();
    }

    private ExperimentoDTOs.RetoResumenResponse toResumen(ExperimentoReto r) {
        return new ExperimentoDTOs.RetoResumenResponse(
                r.getId(),
                r.getNombre(),
                r.getEstado(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getOrganizacion().getId(),
                r.getOrganizacion().getName(),
                r.getCompetition() != null ? r.getCompetition().getId() : null);
    }

    @Transactional
    public ExperimentoDTOs.RetoResumenResponse crearReto(User admin, ExperimentoDTOs.CreateRetoRequest req) {
        OrganizationalGroup org = groupRepo.findById(req.organizacionId())
                .orElseThrow(() -> new NotFoundException("Organización no encontrada: " + req.organizacionId()));
        if (org.getGroupType() != GroupType.EMPRESA) {
            throw new IllegalArgumentException("El reto experimental debe vincularse a una organización (EMPRESA)");
        }
        accessService.requireManage(org);
        if (req.fechaFin().isBefore(req.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior al inicio");
        }

        Competition competition = null;
        if (req.competitionId() != null) {
            competition = resolveLinkableCompetition(req.competitionId(), org, admin);
        }

        int semanas = req.semanasIntervencion() != null ? req.semanasIntervencion() : 8;
        ExperimentoReto reto = ExperimentoReto.builder()
                .nombre(req.nombre().trim())
                .descripcion(req.descripcion())
                .organizacion(org)
                .competition(competition)
                .fechaInicio(req.fechaInicio())
                .fechaFin(req.fechaFin())
                .semanasIntervencion(semanas)
                .estado(ExperimentoRetoEstado.PLANEACION)
                .ambito(ExperimentoAmbito.ORGANIZACION)
                .build();

        return toResumen(retoRepo.save(reto));
    }

    @Transactional
    public ExperimentoDTOs.RetoResumenResponse vincularCompetition(
            Long retoId, User admin, ExperimentoDTOs.VincularCompetitionRequest req) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());

        if (req.competitionId() == null) {
            reto.setCompetition(null);
        } else {
            reto.setCompetition(resolveLinkableCompetition(req.competitionId(), reto.getOrganizacion(), admin));
        }
        return toResumen(retoRepo.save(reto));
    }

    private Competition resolveLinkableCompetition(Long competitionId, OrganizationalGroup org, User admin) {
        Competition competition = competitionRepo.findById(competitionId)
                .orElseThrow(() -> new NotFoundException("Competencia no encontrada: " + competitionId));
        if (competition.getScopeReference() == null) {
            throw new IllegalArgumentException("La competencia no tiene ámbito organizacional definido");
        }
        accessService.requireManage(competition.getScopeReference());
        OrganizationalGroup compRoot = accessService.getRoot(competition.getScopeReference());
        if (compRoot == null || !compRoot.getId().equals(org.getId())) {
            throw new IllegalArgumentException(
                    "La competencia debe pertenecer a la misma organización que el reto experimental");
        }
        return competition;
    }

    @Transactional(readOnly = true)
    public List<com.example.ironplan.rest.dto.CompetitionDTOs.Response> listCompetenciasCandidatas(
            Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());
        return competitionService.listLinkableForOrg(reto.getOrganizacion().getId());
    }

    @Transactional(readOnly = true)
    public List<com.example.ironplan.rest.dto.CompetitionDTOs.Response> listCompetenciasCandidatasPorOrg(
            Long organizacionId, User admin) {
        return competitionService.listLinkableForOrg(organizacionId);
    }

    @Transactional
    public void eliminarReto(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());
        if (reto.getEstado() == ExperimentoRetoEstado.ACTIVO) {
            throw new IllegalStateException("Cierra el reto antes de eliminarlo");
        }

        snapshotRepo.deleteAll(snapshotRepo.findByRetoId(retoId));

        for (ParticipanteReto p : participanteRepo.findByRetoId(retoId)) {
            consentimientoRepo.findByParticipanteRetoId(p.getId()).ifPresent(consentimientoRepo::delete);
            ipaqRepo.findByParticipanteRetoIdAndCorte(p.getId(), IpaqCorte.PRE).ifPresent(ipaqRepo::delete);
            ipaqRepo.findByParticipanteRetoIdAndCorte(p.getId(), IpaqCorte.POST).ifPresent(ipaqRepo::delete);
            susRepo.findByParticipanteRetoId(p.getId()).ifPresent(susRepo::delete);
            participanteRepo.delete(p);
        }

        retoRepo.delete(reto);
    }

    @Transactional(readOnly = true)
    public ExperimentoReto findRetoOrThrow(Long retoId) {
        return retoRepo.findById(retoId)
                .orElseThrow(() -> new NotFoundException("Reto no encontrado: " + retoId));
    }

    @Transactional(readOnly = true)
    public ParticipanteReto findParticipanteOrThrow(Long participanteId) {
        return participanteRepo.findById(participanteId)
                .orElseThrow(() -> new NotFoundException("Participante no encontrado: " + participanteId));
    }

    @Transactional
    public ExperimentoDTOs.InscripcionResponse inscribir(Long retoId, User user, ExperimentoDTOs.InscripcionRequest req) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        if (reto.getEstado() == ExperimentoRetoEstado.CERRADO) {
            throw new IllegalStateException("El reto está cerrado");
        }
        validarObjetivo(req.objetivoCodigo(), req.objetivoTextoLibre());

        ParticipanteReto participante = participanteRepo.findByRetoIdAndUsuarioId(retoId, user.getId())
                .orElseGet(() -> {
                    ParticipanteReto p = ParticipanteReto.builder()
                            .reto(reto)
                            .usuario(user)
                            .categoria(req.categoria())
                            .objetivoCodigo(req.objetivoCodigo())
                            .objetivoTextoLibre(req.objetivoTextoLibre())
                            .build();
                    return participanteRepo.save(p);
                });

        participante.setCategoria(req.categoria());
        participante.setObjetivoCodigo(req.objetivoCodigo());
        participante.setObjetivoTextoLibre(req.objetivoTextoLibre());
        participanteRepo.save(participante);

        return new ExperimentoDTOs.InscripcionResponse(participante.getId());
    }

    @Transactional
    public void registrarConsentimiento(Long retoId, User user, ExperimentoDTOs.ConsentimientoRequest req) {
        ParticipanteReto participante = requireParticipante(retoId, user, req.participanteRetoId());
        if (!Boolean.TRUE.equals(req.acepto())) {
            throw new IllegalArgumentException("Debe aceptar el consentimiento informado");
        }
        if (consentimientoRepo.findByParticipanteRetoId(participante.getId()).isPresent()) {
            return;
        }
        consentimientoRepo.save(ConsentimientoInformado.builder()
                .participanteReto(participante)
                .acepto(true)
                .ipDispositivo(req.ipDispositivo())
                .build());
    }

    @Transactional
    public ExperimentoDTOs.IpaqSubmitResponse guardarIpaq(Long retoId, User user, ExperimentoDTOs.IpaqRequestDto req) {
        ParticipanteReto participante = requireParticipante(retoId, user, req.participanteRetoId());
        requireConsentimiento(participante);

        if (req.corte() == IpaqCorte.POST && !Boolean.TRUE.equals(participante.getReto().getPosttestIpaqActivo())) {
            throw new IllegalStateException("El post-test IPAQ aún no está activo");
        }
        if (ipaqRepo.findByParticipanteRetoIdAndCorte(participante.getId(), req.corte()).isPresent()) {
            throw new IllegalStateException("Ya existe una respuesta IPAQ para este corte");
        }

        ExperimentoDTOs.IpaqResultadoDto resultado = ipaqService.calcular(req);

        ipaqRepo.save(IpaqRespuesta.builder()
                .participanteReto(participante)
                .corte(req.corte())
                .caminataDiasSemana(nvl(req.caminataDiasSemana()))
                .caminataMinDia(nvl(req.caminataMinDia()))
                .moderadaDiasSemana(nvl(req.moderadaDiasSemana()))
                .moderadaMinDia(nvl(req.moderadaMinDia()))
                .vigorosaDiasSemana(nvl(req.vigorosaDiasSemana()))
                .vigorosaMinDia(nvl(req.vigorosaMinDia()))
                .metCaminata(resultado.metCaminata())
                .metModerada(resultado.metModerada())
                .metVigorosa(resultado.metVigorosa())
                .metTotalSemana(resultado.metTotal())
                .categoriaIpaq(resultado.categoriaIpaq())
                .esOutlier(resultado.esOutlier())
                .build());

        if (req.corte() == IpaqCorte.PRE) {
            participante.setCompletoPretest(true);
        } else {
            participante.setCompletoPosttest(true);
        }
        participanteRepo.save(participante);

        return new ExperimentoDTOs.IpaqSubmitResponse(
                resultado.metTotal(), resultado.categoriaIpaq(), resultado.esOutlier());
    }

    @Transactional(readOnly = true)
    public ExperimentoDTOs.IpaqPairResponse getIpaq(Long retoId, User user, Long participanteRetoId) {
        ParticipanteReto participante = requireParticipante(retoId, user, participanteRetoId);
        var pre = ipaqRepo.findByParticipanteRetoIdAndCorte(participante.getId(), IpaqCorte.PRE)
                .map(this::toIpaqView).orElse(null);
        var post = ipaqRepo.findByParticipanteRetoIdAndCorte(participante.getId(), IpaqCorte.POST)
                .map(this::toIpaqView).orElse(null);
        return new ExperimentoDTOs.IpaqPairResponse(pre, post);
    }

    @Transactional
    public ExperimentoDTOs.SusSubmitResponse guardarSus(Long retoId, User user, ExperimentoDTOs.SusRequestDto req) {
        ParticipanteReto participante = requireParticipante(retoId, user, req.participanteRetoId());
        requirePretest(participante);
        if (!Boolean.TRUE.equals(participante.getReto().getSusActivo())) {
            throw new IllegalStateException("La encuesta SUS aún no está activa");
        }
        if (susRepo.findByParticipanteRetoId(participante.getId()).isPresent()) {
            throw new IllegalStateException("Ya completaste la encuesta SUS");
        }

        var puntaje = susService.calcularPuntaje(req);
        var clasificacion = susService.clasificar(puntaje);

        susRepo.save(SusRespuesta.builder()
                .participanteReto(participante)
                .susQ1(req.susQ1()).susQ2(req.susQ2()).susQ3(req.susQ3())
                .susQ4(req.susQ4()).susQ5(req.susQ5()).susQ6(req.susQ6())
                .susQ7(req.susQ7()).susQ8(req.susQ8()).susQ9(req.susQ9())
                .susQ10(req.susQ10())
                .puntajeSus(puntaje)
                .build());

        participante.setCompletoSus(true);
        participanteRepo.save(participante);

        return new ExperimentoDTOs.SusSubmitResponse(puntaje, clasificacion);
    }

    @Transactional(readOnly = true)
    public ExperimentoDTOs.ParticipanteStatusResponse getMiEstado(Long retoId, User user) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        var participante = participanteRepo.findByRetoIdAndUsuarioId(retoId, user.getId()).orElse(null);
        if (participante == null) {
            return new ExperimentoDTOs.ParticipanteStatusResponse(
                    null, retoId, false, false, false, false, true,
                    reto.getPosttestIpaqActivo(), reto.getSusActivo(), null, null);
        }
        boolean tieneConsent = consentimientoRepo.findByParticipanteRetoId(participante.getId()).isPresent();
        return new ExperimentoDTOs.ParticipanteStatusResponse(
                participante.getId(), retoId, tieneConsent,
                participante.getCompletoPretest(), participante.getCompletoPosttest(),
                participante.getCompletoSus(), participante.getActivo(),
                reto.getPosttestIpaqActivo(), reto.getSusActivo(),
                participante.getCategoria(), participante.getObjetivoCodigo());
    }

    @Transactional(readOnly = true)
    public ExperimentoDTOs.ExperimentoEstadoResponse getEstadoAdmin(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());

        return new ExperimentoDTOs.ExperimentoEstadoResponse(
                reto.getId(), reto.getNombre(), reto.getEstado(),
                reto.getFechaInicio(), reto.getFechaFin(),
                reto.getCompetition() != null ? reto.getCompetition().getId() : null,
                participanteRepo.countByRetoId(retoId),
                participanteRepo.countByRetoIdAndCompletoPretestTrue(retoId),
                participanteRepo.countByRetoIdAndCompletoPosttestTrue(retoId),
                participanteRepo.countByRetoIdAndCompletoSusTrue(retoId),
                participanteRepo.countByRetoIdAndActivoTrue(retoId),
                reto.getPosttestIpaqActivo(), reto.getSusActivo());
    }

    @Transactional(readOnly = true)
    public ExperimentoDTOs.SusResumenResponse getSusResumenAdmin(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());

        var respuestas = susRepo.findByRetoId(retoId);
        var promedio = susRepo.avgPuntajeByRetoId(retoId);
        var clasificacion = promedio != null ? susService.clasificar(promedio) : null;

        java.util.Map<String, Long> dist = new java.util.LinkedHashMap<>();
        for (ClasificacionSus c : ClasificacionSus.values()) {
            dist.put(c.name(), 0L);
        }
        for (SusRespuesta s : respuestas) {
            ClasificacionSus c = susService.clasificar(s.getPuntajeSus());
            dist.merge(c.name(), 1L, Long::sum);
        }

        return new ExperimentoDTOs.SusResumenResponse(
                promedio, clasificacion, respuestas.size(), dist);
    }

    @Transactional
    public void activarPosttest(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());
        reto.setPosttestIpaqActivo(true);
        retoRepo.save(reto);
    }

    @Transactional
    public void activarSus(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());
        reto.setSusActivo(true);
        retoRepo.save(reto);
    }

    @Transactional
    public void cerrarReto(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());
        reto.setEstado(ExperimentoRetoEstado.CERRADO);
        retoRepo.save(reto);

        if (reto.getCompetition() != null
                && reto.getCompetition().getStatus() == CompetitionStatus.ACTIVE) {
            competitionService.finish(reto.getCompetition().getId());
        }
    }

    @Transactional
    public void activarReto(Long retoId, User admin) {
        ExperimentoReto reto = findRetoOrThrow(retoId);
        accessService.requireManage(reto.getOrganizacion());
        reto.setEstado(ExperimentoRetoEstado.ACTIVO);
        retoRepo.save(reto);
    }

    /** REGLA 1: bloqueo si no completó pre-test */
    public void requirePretest(ParticipanteReto participante) {
        if (!Boolean.TRUE.equals(participante.getCompletoPretest())) {
            throw new AccessDeniedException("Debe completar el IPAQ pre-test antes de acceder al reto");
        }
    }

    public void requirePretestForUser(Long retoId, User user) {
        ParticipanteReto p = participanteRepo.findByRetoIdAndUsuarioId(retoId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("No estás inscrito en este reto"));
        requirePretest(p);
    }

    private ParticipanteReto requireParticipante(Long retoId, User user, Long participanteRetoId) {
        ParticipanteReto p = findParticipanteOrThrow(participanteRetoId);
        if (!p.getReto().getId().equals(retoId) || !p.getUsuario().getId().equals(user.getId())) {
            throw new AccessDeniedException("Participante no válido para este usuario");
        }
        return p;
    }

    private void requireConsentimiento(ParticipanteReto participante) {
        if (consentimientoRepo.findByParticipanteRetoId(participante.getId()).isEmpty()) {
            throw new IllegalStateException("Debe aceptar el consentimiento informado primero");
        }
    }

    private void validarObjetivo(String codigo, String textoLibre) {
        if (!OBJETIVOS_VALIDOS.contains(codigo)) {
            throw new IllegalArgumentException("Objetivo no válido: " + codigo);
        }
        if ("OBJ-7".equals(codigo) && (textoLibre == null || textoLibre.isBlank())) {
            throw new IllegalArgumentException("OBJ-7 requiere texto libre");
        }
    }

    private int nvl(Integer v) {
        return v != null ? v : 0;
    }

    private ExperimentoDTOs.IpaqRespuestaView toIpaqView(IpaqRespuesta r) {
        return new ExperimentoDTOs.IpaqRespuestaView(
                r.getCorte(),
                r.getCaminataDiasSemana(), r.getCaminataMinDia(),
                r.getModeradaDiasSemana(), r.getModeradaMinDia(),
                r.getVigorosaDiasSemana(), r.getVigorosaMinDia(),
                r.getMetTotalSemana(), r.getCategoriaIpaq(), r.getEsOutlier(),
                r.getFechaAplicacion());
    }
}
