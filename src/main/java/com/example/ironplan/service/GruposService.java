package com.example.ironplan.service;

import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.GroupType;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.CompetitionRepository;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import com.example.ironplan.rest.dto.GrupoDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GruposService {

    private final GroupMembershipService membershipService;
    private final OrganizationalAccessService accessService;
    private final OrganizationalGroupRepository groupRepo;
    private final CompetitionRepository competitionRepo;
    private final CompetitionService competitionService;
    private final GrupoMetricasService metricasService;

    @Transactional(readOnly = true)
    public List<GrupoDTOs.MembershipSummary> listMisGrupos(User user) {
        List<GrupoDTOs.MembershipSummary> list = membershipService.listMisGrupos(user);
        list.forEach(this::enrichCompetitionCount);
        return list;
    }

    @Transactional(readOnly = true)
    public List<GrupoDTOs.MembershipSummary> listAdministrar(User user) {
        List<GrupoDTOs.MembershipSummary> list = membershipService.listAdministrarGrupos(user);
        list.forEach(this::enrichCompetitionCount);
        return list;
    }

    @Transactional(readOnly = true)
    public GrupoDTOs.GroupDetail getDetail(Long groupId, User user) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        accessService.requireView(group);

        var role = membershipService.resolveRoleForUser(user, group);
        int activeCompetitions = countActiveCompetitions(group.getId());

        return GrupoDTOs.GroupDetail.builder()
            .groupId(group.getId())
            .groupName(group.getName())
            .groupCode(group.getCode())
            .groupType(group.getGroupType().name())
            .active(group.getActive())
            .role(role != null ? role.name() : null)
            .canManage(accessService.canManageGroup(user, group.getId()))
            .memberCount(membershipService.countMembersForScope(groupId))
            .activeCompetitionsCount(activeCompetitions)
            .hierarchyPath(membershipService.buildHierarchyPath(group))
            .build();
    }

    @Transactional(readOnly = true)
    public List<GrupoDTOs.MemberItem> listMembers(Long groupId, User user) {
        accessService.requireView(groupId);
        return membershipService.listGroupMembers(groupId);
    }

    @Transactional
    public GrupoDTOs.MemberItem addMember(Long groupId, User user, GrupoDTOs.AddMemberRequest req) {
        return membershipService.addMember(groupId, user, req);
    }

    @Transactional
    public void removeMember(Long groupId, Long targetUserId, User user) {
        membershipService.removeMember(groupId, targetUserId, user);
    }

    @Transactional
    public GrupoDTOs.MemberItem updateMemberRole(Long groupId, Long targetUserId, User user, GrupoDTOs.UpdateMemberRoleRequest req) {
        return membershipService.updateMemberRole(groupId, targetUserId, req);
    }

    @Transactional(readOnly = true)
    public List<CompetitionDTOs.RetoSummary> listRetos(Long groupId, User user) {
        accessService.requireView(groupId);
        return competitionService.getRetosForOrganizationalGroup(groupId);
    }

    @Transactional(readOnly = true)
    public CompetitionDTOs.CompetitionDetailView getRetoDetail(Long groupId, Long competitionId, User user) {
        accessService.requireView(groupId);
        competitionService.ensureScoresFresh(competitionId);
        return competitionService.getDetailForUser(competitionId, user, groupId);
    }

    @Transactional
    public CompetitionDTOs.Response createReto(Long groupId, User user, GrupoDTOs.CreateRetoRequest req) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        if (!accessService.canManageGroup(user, groupId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "No tienes permiso para crear retos en este grupo");
        }
        if (group.getGroupType() != GroupType.GRUPO) {
            throw new IllegalArgumentException(
                "Los retos entre miembros del mismo grupo solo pueden crearse en un grupo hoja (tipo GRUPO)");
        }

        CompetitionDTOs.CreateRequest createReq = new CompetitionDTOs.CreateRequest();
        createReq.setName(req.getName());
        createReq.setCompetitionType(req.getCompetitionType());
        createReq.setMetricType(req.getMetricType());
        createReq.setStartDate(req.getStartDate());
        createReq.setEndDate(req.getEndDate());
        createReq.setParticipantUserIds(req.getParticipantUserIds());

        return competitionService.createIntraGroupReto(groupId, createReq);
    }

    @Transactional
    public CompetitionDTOs.Response activateReto(Long groupId, Long competitionId, User user) {
        if (!accessService.canManageGroup(user, groupId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "No tienes permiso para activar retos en este grupo");
        }
        CompetitionDTOs.Response reto = competitionService.getById(competitionId);
        if (!reto.isMemberCompetition() || !groupId.equals(reto.getScopeReferenceId())) {
            throw new IllegalArgumentException("Este reto no pertenece al grupo seleccionado");
        }
        return competitionService.activate(competitionId);
    }

    @Transactional(readOnly = true)
    public GrupoDTOs.GroupMetrics getMetricas(Long groupId, User user, int periodDays) {
        return metricasService.getMetrics(groupId, user, periodDays);
    }

    private void enrichCompetitionCount(GrupoDTOs.MembershipSummary summary) {
        summary.setActiveCompetitionsCount(countActiveCompetitions(summary.getGroupId()));
    }

    private int countActiveCompetitions(Long groupId) {
        return competitionService.findActiveCompetitionsVisibleFromGroup(groupId).size();
    }
}
