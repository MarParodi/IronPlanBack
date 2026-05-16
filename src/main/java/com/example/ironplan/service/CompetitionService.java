package com.example.ironplan.service;
 
import com.example.ironplan.model.*;
import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class CompetitionService {
 
    private final CompetitionRepository                  competitionRepo;
    private final CompetitionParticipantRepository       participantRepo;
    private final CompetitionMemberParticipantRepository memberParticipantRepo;
    private final OrganizationalGroupRepository          groupRepo;
    private final UserActivityRepository                 activityRepo;
    private final UserRepository                         userRepo;
 
    // ─── Admin: Listar ────────────────────────────────────────────────────────
 
    public List<CompetitionDTOs.Response> getAll(CompetitionStatus status, CompetitionType type) {
        List<Competition> list;
        if (status != null && type != null)      list = competitionRepo.findByStatusAndCompetitionType(status, type);
        else if (status != null)                 list = competitionRepo.findByStatus(status);
        else if (type != null)                   list = competitionRepo.findByCompetitionType(type);
        else                                     list = competitionRepo.findAll();
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }
 
    public CompetitionDTOs.Response getById(Long id) {
        return toResponse(findOrThrow(id));
    }
 
    // ─── Admin: Crear ─────────────────────────────────────────────────────────
 
    @Transactional
    public CompetitionDTOs.Response create(CompetitionDTOs.CreateRequest req) {
        if (req.getCompetitionType() == CompetitionType.CHALLENGE && req.getEndDate() == null) {
            throw new IllegalArgumentException("CHALLENGE requiere una fecha de fin");
        }
 
        OrganizationalGroup scopeRef = groupRepo.findById(req.getScopeReferenceId())
            .orElseThrow(() -> new RuntimeException("Nodo de scope no encontrado"));
 
        Competition competition = Competition.builder()
            .name(req.getName())
            .competitionType(req.getCompetitionType())
            .scopeLevel(req.getScopeLevel())
            .scopeReference(scopeRef)
            .metricType(req.getMetricType())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .status(CompetitionStatus.DRAFT)
            .createdBy(getCurrentUser())
            .build();
 
        Competition saved = competitionRepo.save(competition);
 
        if (req.getScopeLevel() == ScopeLevel.GRUPO) {
            enrollMemberParticipants(saved, scopeRef.getId(), req.getParticipantUserIds());
        } else {
            List<Long> groupIds = resolveGroupParticipants(req, scopeRef.getId());
            validateGroupParticipants(req.getCompetitionType(), groupIds, scopeRef.getId());
            enrollGroupParticipants(saved, groupIds);
        }
 
        return toResponse(saved);
    }
 
    // ─── Admin: Activar / Finalizar ───────────────────────────────────────────
 
    @Transactional
    public CompetitionDTOs.Response activate(Long id) {
        Competition c = findOrThrow(id);
        if (c.getStatus() != CompetitionStatus.DRAFT)
            throw new IllegalStateException("Solo se puede activar una competencia en DRAFT");
        c.activate();
        return toResponse(competitionRepo.save(c));
    }
 
    @Transactional
    public CompetitionDTOs.Response finish(Long id) {
        Competition c = findOrThrow(id);
        if (c.getStatus() != CompetitionStatus.ACTIVE)
            throw new IllegalStateException("Solo se puede finalizar una competencia ACTIVE");
        recalculateScores(c);
        c.finish();
        return toResponse(competitionRepo.save(c));
    }
 
    // ─── Público: Competencias activas del usuario ────────────────────────────
 
    public List<CompetitionDTOs.Response> getActiveForUser(User user) {
        if (user.getPrimaryOrganizationalGroup() == null) return List.of();
        return competitionRepo
            .findActiveCompetitionsForGroup(user.getPrimaryOrganizationalGroup().getId())
            .stream().map(this::toResponse).collect(Collectors.toList());
    }
    
    private int countMembersRecursive(Long groupId) {
        int count = userRepo.countByPrimaryOrganizationalGroupId(groupId);
        List<OrganizationalGroup> children = groupRepo.findByParentId(groupId);
        for (OrganizationalGroup child : children) {
            count += countMembersRecursive(child.getId());
        }
        return count;
    }
 
    // ─── Público: Leaderboard grupal ──────────────────────────────────────────
 
    public List<CompetitionDTOs.LeaderboardEntry> getLeaderboard(Long competitionId) {
        Competition c = findOrThrow(competitionId);
        if (c.getScopeLevel() == ScopeLevel.GRUPO)
            throw new IllegalArgumentException("Esta competencia es individual, usa /leaderboard/members");
 
        AtomicInteger rank = new AtomicInteger(1);
        return participantRepo.findLeaderboard(competitionId).stream()
            .map(p -> CompetitionDTOs.LeaderboardEntry.builder()
                .rank(rank.getAndIncrement())
                .groupId(p.getGroup().getId())
                .groupName(p.getGroup().getName())
                .groupScore(p.getGroupScore())
                .activeMembers(countMembersRecursive(p.getGroup().getId()))
                .build())
            .collect(Collectors.toList());
    }
    
    public List<CompetitionDTOs.ScopeNodeDetail> getMembersUnderGroup(Long groupId) {
        List<User> users = new ArrayList<>();
        collectUsersRecursive(groupId, users);
        return users.stream()
            .map(u -> CompetitionDTOs.ScopeNodeDetail.builder()
                .id(u.getId())
                .name(u.getFirstName() + " " + u.getLastName())
                .groupType("MEMBER")
                .isLeaf(true)
                .memberCount(0)
                .children(List.of())
                .build())
            .collect(Collectors.toList());
    }

    private void collectUsersRecursive(Long groupId, List<User> result) {
        result.addAll(userRepo.findByPrimaryOrganizationalGroupId(groupId));
        groupRepo.findByParentId(groupId)
            .forEach(child -> collectUsersRecursive(child.getId(), result));
    }
 
    // ─── Público: Leaderboard individual ─────────────────────────────────────
 
    public List<CompetitionDTOs.MemberLeaderboardEntry> getMemberLeaderboard(Long competitionId) {
        Competition c = findOrThrow(competitionId);
        if (c.getScopeLevel() != ScopeLevel.GRUPO)
            throw new IllegalArgumentException("Esta competencia es grupal, usa /leaderboard");
 
        AtomicInteger rank = new AtomicInteger(1);
        return memberParticipantRepo.findLeaderboard(competitionId).stream()
            .map(p -> CompetitionDTOs.MemberLeaderboardEntry.builder()
                .rank(rank.getAndIncrement())
                .userId(p.getUser().getId())
                .fullName(p.getUser().getFirstName() + " " + p.getUser().getLastName())
                .username(p.getUser().getDisplayUsername())
                .profilePictureUrl(p.getUser().getProfilePictureUrl())
                .score(p.getScore())
                .build())
            .collect(Collectors.toList());
    }
 
    // ─── Público: Ranking interno del grupo del usuario ───────────────────────
 
    @Transactional(readOnly = true)
    public List<CompetitionDTOs.InternalRankingEntry> getInternalRanking(Long competitionId, User user) {
        Competition c = findOrThrow(competitionId);
        if (c.getScopeLevel() == ScopeLevel.GRUPO)
            throw new IllegalArgumentException("Competencia individual no tiene ranking interno de grupo");

        User fullUser = userRepo.findById(user.getId()).orElse(user);
        if (fullUser.getPrimaryOrganizationalGroup() == null) return List.of();

        // Buscar qué ancestro del usuario participa en la competencia
        Long participantGroupId = findParticipantAncestor(
            fullUser.getPrimaryOrganizationalGroup(), competitionId
        );

        if (participantGroupId == null) return List.of(); // no lanzar error, solo retornar vacío

        LocalDate start = c.getStartDate();
        LocalDate end   = c.getEndDate() != null ? c.getEndDate() : LocalDate.now();

        AtomicInteger pos = new AtomicInteger(1);
        return activityRepo.findInternalRanking(participantGroupId, c.getMetricType(), start, end).stream()
            .map(r -> CompetitionDTOs.InternalRankingEntry.builder()
                .position(pos.getAndIncrement())
                .userId((Long) r[0])
                .fullName(r[1] + " " + r[2])
                .username((String) r[3])
                .profilePictureUrl((String) r[4])
                .score((Double) r[5])
                .build())
            .collect(Collectors.toList());
    }

    // Sube por la jerarquía del grupo del usuario hasta encontrar uno que participe
    private Long findParticipantAncestor(OrganizationalGroup group, Long competitionId) {
        OrganizationalGroup current = group;
        int maxDepth = 5;
        while (current != null && maxDepth-- > 0) {
            if (participantRepo.existsByCompetitionIdAndGroupId(competitionId, current.getId())) {
                return current.getId();
            }
            current = current.getParent();
        }
        return null;
    }
 
    // ─── Público: Mi score ────────────────────────────────────────────────────
 
    @Transactional(readOnly = true)
    public CompetitionDTOs.MyScore getMyScore(Long competitionId, User user) {

        User fullUser = userRepo.findById(user.getId()).orElse(user);
        Competition c = findOrThrow(competitionId);

        // Competencia individual
        if (c.getScopeLevel() == ScopeLevel.GRUPO) {
            var myEntry = memberParticipantRepo
                .findByCompetitionIdAndUserId(competitionId, fullUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("No participas en esta competencia"));

            List<CompetitionMemberParticipant> all = memberParticipantRepo.findLeaderboard(competitionId);
            int myRank = 1;
            for (CompetitionMemberParticipant p : all) {
                if (p.getUser().getId().equals(fullUser.getId())) break;
                myRank++;
            }
            return CompetitionDTOs.MyScore.builder()
                .memberRank(myRank)
                .individualScore(myEntry.getScore())
                .isMemberCompetition(true)
                .build();
        }

        // Competencia grupal
        if (fullUser.getPrimaryOrganizationalGroup() == null)
            throw new IllegalArgumentException("No perteneces a ningún grupo");

        Long groupId = fullUser.getPrimaryOrganizationalGroup().getId();
        List<CompetitionParticipant> leaderboard = participantRepo.findLeaderboard(competitionId);

        int groupRank = 1;
        Double groupScore = 0.0;
        for (CompetitionParticipant p : leaderboard) {
            if (p.getGroup().getId().equals(groupId)) { groupScore = p.getGroupScore(); break; }
            groupRank++;
        }

        LocalDate start = c.getStartDate();
        LocalDate end   = c.getEndDate() != null ? c.getEndDate() : LocalDate.now();
        List<Object[]> internalRows = activityRepo.findInternalRanking(groupId, c.getMetricType(), start, end);

        int internalRank = 1;
        Double individualScore = 0.0;
        for (Object[] row : internalRows) {
            if (((Long) row[0]).equals(fullUser.getId())) { individualScore = (Double) row[5]; break; }
            internalRank++;
        }

        return CompetitionDTOs.MyScore.builder()
            .groupRank(groupRank)
            .groupScore(groupScore)
            .internalRank(internalRank)
            .individualScore(individualScore)
            .groupName(fullUser.getPrimaryOrganizationalGroup().getName())
            .isMemberCompetition(false)
            .build();
    }
 
    // ─── Navegación del selector de scope ─────────────────────────────────────
 
    public List<CompetitionDTOs.ScopeNodeDetail> getScopeChildren(Long groupId) {
        return groupRepo.findByParentId(groupId).stream()
            .filter(OrganizationalGroup::getActive)
            .map(g -> {
                boolean isLeaf = g.getGroupType() == GroupType.GRUPO;
                int memberCount = isLeaf
                    ? userRepo.countByPrimaryOrganizationalGroupId(g.getId())
                    : 0;
                return CompetitionDTOs.ScopeNodeDetail.builder()
                    .id(g.getId())
                    .name(g.getName())
                    .groupType(g.getGroupType().name())
                    .isLeaf(isLeaf)
                    .memberCount(memberCount)
                    .children(List.of())
                    .build();
            }).collect(Collectors.toList());
    }
 
    // Miembros de un grupo para el selector de participantes individuales
    public List<CompetitionDTOs.ScopeNodeDetail> getGroupMembers(Long groupId) {
        return userRepo.findByPrimaryOrganizationalGroupId(groupId).stream()
            .map(u -> CompetitionDTOs.ScopeNodeDetail.builder()
                .id(u.getId())
                .name(u.getFirstName() + " " + u.getLastName())
                .groupType("MEMBER")
                .isLeaf(true)
                .memberCount(0)
                .children(List.of())
                .build())
            .collect(Collectors.toList());
    }
 
    // ─── Scheduler ────────────────────────────────────────────────────────────
 
 // NUEVO - recalcula todas las activas Y cierra las expiradas
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void dailyCompetitionUpdate() {
        // 1. Recalcular scores de todas las competencias ACTIVE
        List<Competition> active = competitionRepo.findByStatus(CompetitionStatus.ACTIVE);
        active.forEach(c -> recalculateScores(c));
        if (!active.isEmpty()) competitionRepo.saveAll(active);

        // 2. Cerrar las que ya expiraron
        List<Competition> expired = active.stream()
            .filter(c -> c.getEndDate() != null && c.getEndDate().isBefore(LocalDate.now()))
            .toList();
        expired.forEach(c -> c.finish());
        if (!expired.isEmpty()) competitionRepo.saveAll(expired);
    }

    
 
    // ─── Helpers privados ─────────────────────────────────────────────────────
 
    private void enrollGroupParticipants(Competition competition, List<Long> groupIds) {
        List<CompetitionParticipant> participants = new ArrayList<>();
        for (Long gid : groupIds) {
            OrganizationalGroup group = groupRepo.findById(gid)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + gid));
            participants.add(CompetitionParticipant.builder()
                .competition(competition).group(group).groupScore(0.0).build());
        }
        participantRepo.saveAll(participants);
    }
 
    private void enrollMemberParticipants(Competition competition, Long groupId, List<Long> userIds) {
        List<User> users = (userIds != null && !userIds.isEmpty())
            ? userRepo.findAllById(userIds)
            : userRepo.findByPrimaryOrganizationalGroupId(groupId);
 
        if (users.size() < 2)
            throw new IllegalArgumentException("Se requieren al menos 2 participantes");
        if (competition.getCompetitionType() == CompetitionType.VERSUS && users.size() != 2)
            throw new IllegalArgumentException("VERSUS requiere exactamente 2 participantes");
 
        memberParticipantRepo.saveAll(users.stream()
            .map(u -> CompetitionMemberParticipant.builder()
                .competition(competition).user(u).score(0.0).build())
            .collect(Collectors.toList()));
    }
 
    private List<Long> resolveGroupParticipants(CompetitionDTOs.CreateRequest req, Long scopeRefId) {
        if (req.getParticipantGroupIds() != null && !req.getParticipantGroupIds().isEmpty())
            return req.getParticipantGroupIds();
        return groupRepo.findLeafGroupsUnder(scopeRefId)
            .stream().map(OrganizationalGroup::getId).collect(Collectors.toList());
    }
 
    private void validateGroupParticipants(CompetitionType type, List<Long> groupIds, Long scopeRefId) {
        if (groupIds.size() < 2)
            throw new IllegalArgumentException("Se requieren al menos 2 grupos");
        if (type == CompetitionType.VERSUS && groupIds.size() != 2)
            throw new IllegalArgumentException("VERSUS requiere exactamente 2 grupos");
        
        // Verificar que todos pertenecen al scope
        List<Long> validIds = groupRepo.findAllById(groupIds).stream()
            .filter(g -> isUnderScope(g, scopeRefId))
            .map(g -> g.getId())
            .toList();
        
        if (validIds.size() != groupIds.size())
            throw new IllegalArgumentException("Uno o más grupos están fuera del scope");
    }

    private boolean isUnderScope(OrganizationalGroup g, Long scopeRefId) {
        OrganizationalGroup current = g;
        int maxDepth = 5;
        while (current != null && maxDepth-- > 0) {
            if (current.getId().equals(scopeRefId)) return true;
            current = current.getParent();
        }
        return false;
    }
 
    private void recalculateScores(Competition c) {
        LocalDate start = c.getStartDate();
        LocalDate end   = c.getEndDate() != null ? c.getEndDate() : LocalDate.now();
 
        if (c.getScopeLevel() == ScopeLevel.GRUPO) {
            List<CompetitionMemberParticipant> members = memberParticipantRepo.findLeaderboard(c.getId());
            int rank = 1;
            for (CompetitionMemberParticipant p : members) {
                Double score = activityRepo.sumUserScore(p.getUser().getId(), c.getMetricType(), start, end);
                p.setScore(score != null ? score : 0.0);
                p.setRank(rank++);
                p.setLastCalculatedAt(LocalDateTime.now());
            }
            memberParticipantRepo.saveAll(members);
        } else {
            List<CompetitionParticipant> participants = participantRepo.findLeaderboard(c.getId());
            int rank = 1;
            for (CompetitionParticipant p : participants) {
                Double score = activityRepo.sumGroupScore(p.getGroup().getId(), c.getMetricType().name(), start, end);
                p.setGroupScore(score != null ? score : 0.0);
                p.setRank(rank++);
                p.setLastCalculatedAt(LocalDateTime.now());
            }
            participantRepo.saveAll(participants);
        }
    }
 
    private Competition findOrThrow(Long id) {
        return competitionRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Competencia no encontrada: " + id));
    }
 
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
 
    private CompetitionDTOs.Response toResponse(Competition c) {
        return CompetitionDTOs.Response.builder()
            .id(c.getId())
            .name(c.getName())
            .competitionType(c.getCompetitionType())
            .scopeLevel(c.getScopeLevel())
            .scopeReferenceId(c.getScopeReference().getId())
            .scopeReferenceName(c.getScopeReference().getName())
            .metricType(c.getMetricType())
            .startDate(c.getStartDate())
            .endDate(c.getEndDate())
            .status(c.getStatus())
            .createdAt(c.getCreatedAt())
            .participantCount(c.getParticipants().size())
            .isMemberCompetition(c.getScopeLevel() == ScopeLevel.GRUPO)
            .build();
    }
    
    
    
    
    @Transactional
    public void recalculateScoresManual(Long competitionId) {
        Competition c = findOrThrow(competitionId);
        recalculateScores(c);
        competitionRepo.save(c);
    }
    
    @Transactional(readOnly = true)
    public List<CompetitionDTOs.Response> getAllForUser(User user) {
        User fullUser = userRepo.findById(user.getId()).orElse(user);
        if (fullUser.getPrimaryOrganizationalGroup() == null) return List.of();
        
        Long groupId = fullUser.getPrimaryOrganizationalGroup().getId();
        
        // Buscar en todos los ancestros
        List<Long> ancestorIds = new ArrayList<>();
        var current = fullUser.getPrimaryOrganizationalGroup();
        int maxDepth = 5;
        while (current != null && maxDepth-- > 0) {
            ancestorIds.add(current.getId());
            current = current.getParent();
        }
        
        return competitionRepo.findAllByParticipantGroupIds(ancestorIds)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }
    
    
    
}