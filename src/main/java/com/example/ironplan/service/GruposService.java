package com.example.ironplan.service;

import com.example.ironplan.model.OrganizationalGroup;
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
        return competitionService.getDetailForUser(competitionId, user, groupId);
    }

    @Transactional(readOnly = true)
    public GrupoDTOs.GroupMetrics getMetricas(Long groupId, User user, int periodDays) {
        return metricasService.getMetrics(groupId, user, periodDays);
    }

    private void enrichCompetitionCount(GrupoDTOs.MembershipSummary summary) {
        summary.setActiveCompetitionsCount(countActiveCompetitions(summary.getGroupId()));
    }

    private int countActiveCompetitions(Long groupId) {
        return competitionRepo.findActiveCompetitionsForGroup(groupId).size();
    }
}
