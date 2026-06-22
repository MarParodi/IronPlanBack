package com.example.ironplan.service;
 
import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ironplan.config.CacheConfig;
import com.example.ironplan.config.LeaderboardCacheEvictor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class CompetitionService {

    private static final int SCORE_REFRESH_SECONDS = 60;
 
    private final CompetitionRepository                  competitionRepo;
    private final CompetitionParticipantRepository       participantRepo;
    private final CompetitionMemberParticipantRepository memberParticipantRepo;
    private final OrganizationalGroupRepository          groupRepo;
    private final UserActivityRepository                 activityRepo;
    private final UserRepository                         userRepo;
    private final OrganizationalAccessService            accessService;
    private final OrganizationalGroupMemberRepository    memberRepo;
    private final NotificationService                    notificationService;
    private final LeaderboardCacheEvictor                leaderboardCacheEvictor;
 
    // ─── Admin: Listar ────────────────────────────────────────────────────────
 
    public List<CompetitionDTOs.Response> getAll(CompetitionStatus status, CompetitionType type) {
        List<Competition> list;
        if (status != null && type != null)      list = competitionRepo.findByStatusAndCompetitionType(status, type);
        else if (status != null)                 list = competitionRepo.findByStatus(status);
        else if (type != null)                   list = competitionRepo.findByCompetitionType(type);
        else                                     list = competitionRepo.findAll();
        User user = accessService.getCurrentUser();
        return list.stream()
            .filter(c -> c.getScopeReference() == null || accessService.canView(user, c.getScopeReference()))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /** Competencias vinculables a un reto experimental de la misma organización raíz. */
    public List<CompetitionDTOs.Response> listLinkableForOrg(Long organizacionId) {
        OrganizationalGroup org = groupRepo.findById(organizacionId)
                .orElseThrow(() -> new RuntimeException("Organización no encontrada: " + organizacionId));
        accessService.requireManage(org);
        OrganizationalGroup orgRoot = accessService.getRoot(org);
        if (orgRoot == null) {
            throw new IllegalArgumentException("No se pudo resolver la organización raíz");
        }
        final Long rootId = orgRoot.getId();
        User user = accessService.getCurrentUser();
        return competitionRepo.findAll().stream()
                .filter(c -> c.getScopeReference() != null)
                .filter(c -> {
                    OrganizationalGroup compRoot = accessService.getRoot(c.getScopeReference());
                    return compRoot != null && compRoot.getId().equals(rootId);
                })
                .filter(c -> accessService.canManage(user, c.getScopeReference()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
 
    public CompetitionDTOs.Response getById(Long id) {
        Competition competition = findOrThrow(id);
        if (competition.getScopeReference() != null) {
            accessService.requireView(competition.getScopeReference());
        }
        return toResponse(competition);
    }
 
    // ─── Admin: Crear ─────────────────────────────────────────────────────────
 
    @Transactional
    public CompetitionDTOs.Response create(CompetitionDTOs.CreateRequest req) {
        if (req.getCompetitionType() == CompetitionType.CHALLENGE) {
            if (req.getEndDate() == null) {
                throw new IllegalArgumentException("CHALLENGE requiere fecha de inicio y fin");
            }
            if (req.getEndDate().isBefore(req.getStartDate())) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior al inicio");
            }
        }
 
        OrganizationalGroup scopeRef = groupRepo.findById(req.getScopeReferenceId())
            .orElseThrow(() -> new RuntimeException("Nodo de scope no encontrado"));
        accessService.requireManage(scopeRef);

        if (req.getScopeLevel() == ScopeLevel.GRUPO && scopeRef.getGroupType() != GroupType.GRUPO) {
            throw new IllegalArgumentException(
                "Los retos entre miembros del mismo grupo deben crearse en un grupo hoja (tipo GRUPO). "
                    + "Navega hasta el grupo concreto y selecciona a los miembros participantes.");
        }

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
            .participantMode(req.getParticipantMode() != null
                    ? req.getParticipantMode()
                    : ParticipantMode.GROUP)
            .build();
 
        Competition saved = competitionRepo.save(competition);

        boolean orgMembers = saved.getParticipantMode() == ParticipantMode.ORGANIZATION_MEMBERS;
 
        boolean hasGroupParticipants = req.getParticipantGroupIds() != null
            && !req.getParticipantGroupIds().isEmpty();
        boolean hasMemberParticipants = req.getParticipantUserIds() != null
            && !req.getParticipantUserIds().isEmpty();

        if (orgMembers) {
            enrollOrganizationMembers(saved, scopeRef.getId(), req.getParticipantUserIds());
        } else if (hasGroupParticipants && hasMemberParticipants) {
            throw new IllegalArgumentException(
                "Indica solo grupos participantes o solo miembros, no ambos");
        } else if (hasGroupParticipants) {
            validateGroupParticipants(req.getCompetitionType(), req.getParticipantGroupIds(), scopeRef.getId());
            enrollGroupParticipants(saved, req.getParticipantGroupIds());
        } else if (req.getScopeLevel() == ScopeLevel.GRUPO || hasMemberParticipants) {
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
        requireManageCompetition(c);
        if (c.getStatus() != CompetitionStatus.DRAFT)
            throw new IllegalStateException("Solo se puede activar una competencia en DRAFT");
        c.activate();
        return toResponse(competitionRepo.save(c));
    }
 
    @Transactional
    public CompetitionDTOs.Response finish(Long id) {
        Competition c = findOrThrow(id);
        requireManageCompetition(c);
        if (c.getStatus() != CompetitionStatus.ACTIVE)
            throw new IllegalStateException("Solo se puede finalizar una competencia ACTIVE");
        recalculateScores(c);
        c.finish();
        return toResponse(competitionRepo.save(c));
    }
 
    // ─── Público: Competencias activas del usuario ────────────────────────────
 
    public List<CompetitionDTOs.Response> getActiveForUser(User user) {
        User fullUser = userRepo.findById(user.getId()).orElse(user);
        Map<Long, Competition> byId = new LinkedHashMap<>();

        if (fullUser.getPrimaryOrganizationalGroup() != null) {
            findActiveCompetitionsVisibleFromGroup(fullUser.getPrimaryOrganizationalGroup().getId())
                .forEach(c -> byId.put(c.getId(), c));
        }
        competitionRepo.findActiveMemberCompetitionsForUser(fullUser.getId())
            .forEach(c -> byId.put(c.getId(), c));

        return byId.values().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CompetitionDTOs.Response> getActiveForOrganizationalGroup(Long groupId) {
        return findActiveCompetitionsVisibleFromGroup(groupId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /** Competencias ACTIVE visibles desde un nodo organizacional (incluye ranking org-wide). */
    @Transactional(readOnly = true)
    public List<Competition> findActiveCompetitionsVisibleFromGroup(Long groupId) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        OrganizationalGroup root = accessService.getRoot(group);
        if (root == null) {
            root = group;
        }
        List<Long> treeIds = accessService.collectTreeGroupIds(root.getId());
        return competitionRepo.findVisibleForOrganizationalScope(
            treeIds, List.of(CompetitionStatus.ACTIVE));
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
        return getLeaderboard(competitionId, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.LEADERBOARD, key = "#competitionId")
    public List<CompetitionDTOs.LeaderboardEntry> getLeaderboard(Long competitionId, User viewer) {
        Competition c = findOrThrow(competitionId);
        if (viewer != null) requireCompetitionView(c, viewer);
        if (isMemberCompetition(c))
            throw new IllegalArgumentException("Esta competencia es individual, usa /leaderboard/members");

        refreshScoresIfNeeded(c);

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
        accessService.requireView(groupId);
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
        return getMemberLeaderboard(competitionId, null);
    }

    public List<CompetitionDTOs.MemberLeaderboardEntry> getMemberLeaderboard(Long competitionId, User viewer) {
        return getMemberLeaderboard(competitionId, viewer, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = CacheConfig.MEMBER_LEADERBOARD,
        key = "#competitionId + '-' + (#levelFilter != null ? #levelFilter.name() : 'ALL')"
    )
    public List<CompetitionDTOs.MemberLeaderboardEntry> getMemberLeaderboard(
            Long competitionId, User viewer, Level levelFilter) {
        Competition c = findOrThrow(competitionId);
        if (viewer != null) requireCompetitionView(c, viewer);
        if (!isMemberCompetition(c))
            throw new IllegalArgumentException("Esta competencia es grupal, usa /leaderboard");

        refreshScoresIfNeeded(c);

        AtomicInteger rank = new AtomicInteger(1);
        return memberParticipantRepo.findLeaderboard(competitionId).stream()
            .map(p -> p)
            .filter(p -> levelFilter == null || levelFilter.equals(p.getUser().getLevel()))
            .map(p -> CompetitionDTOs.MemberLeaderboardEntry.builder()
                .rank(rank.getAndIncrement())
                .userId(p.getUser().getId())
                .fullName(p.getUser().getFirstName() + " " + p.getUser().getLastName())
                .username(p.getUser().getDisplayUsername())
                .profilePictureUrl(p.getUser().getProfilePictureUrl())
                .score(p.getScore())
                .level(p.getUser().getLevel())
                .build())
            .collect(Collectors.toList());
    }
 
    // ─── Público: Ranking interno del grupo del usuario ───────────────────────
 
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.INTERNAL_RANKING, key = "#competitionId + '-' + #user.id")
    public List<CompetitionDTOs.InternalRankingEntry> getInternalRanking(Long competitionId, User user) {
        Competition c = findOrThrow(competitionId);
        requireCompetitionView(c, user);
        if (isMemberCompetition(c))
            throw new IllegalArgumentException("Competencia individual no tiene ranking interno de grupo");

        refreshScoresIfNeeded(c);

        User fullUser = userRepo.findById(user.getId()).orElse(user);
        if (fullUser.getPrimaryOrganizationalGroup() == null) return List.of();

        // Buscar qué ancestro del usuario participa en la competencia
        Long participantGroupId = findParticipantAncestor(
            fullUser.getPrimaryOrganizationalGroup(), competitionId
        );

        if (participantGroupId == null) return List.of();

        return buildInternalRankingEntries(participantGroupId, c);
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
        requireCompetitionView(c, fullUser);
        refreshScoresIfNeeded(c);

        // Competencia individual
        if (isMemberCompetition(c)) {
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

        // Competencia grupal: el equipo participante puede ser un ancestro del grupo primario
        if (fullUser.getPrimaryOrganizationalGroup() == null)
            throw new IllegalArgumentException("No perteneces a ningún grupo");

        Long participantGroupId = findParticipantAncestor(
            fullUser.getPrimaryOrganizationalGroup(), competitionId);
        if (participantGroupId == null)
            throw new IllegalArgumentException("Tu grupo no participa en esta competencia");

        List<CompetitionParticipant> leaderboard = participantRepo.findLeaderboard(competitionId);

        int groupRank = 1;
        Double groupScore = 0.0;
        String groupName = null;
        for (CompetitionParticipant p : leaderboard) {
            if (p.getGroup().getId().equals(participantGroupId)) {
                groupScore = p.getGroupScore();
                groupName = p.getGroup().getName();
                break;
            }
            groupRank++;
        }

        List<CompetitionDTOs.InternalRankingEntry> internalRows =
            buildInternalRankingEntries(participantGroupId, c);

        int internalRank = 1;
        Double individualScore = 0.0;
        for (CompetitionDTOs.InternalRankingEntry row : internalRows) {
            if (row.getUserId().equals(fullUser.getId())) {
                individualScore = row.getScore();
                break;
            }
            internalRank++;
        }

        if (groupName == null) {
            groupName = groupRepo.findById(participantGroupId)
                .map(OrganizationalGroup::getName)
                .orElse(null);
        }

        return CompetitionDTOs.MyScore.builder()
            .groupRank(groupRank)
            .groupScore(groupScore)
            .internalRank(internalRank)
            .individualScore(individualScore)
            .groupName(groupName)
            .participantGroupId(participantGroupId)
            .isMemberCompetition(false)
            .build();
    }
 
    // ─── Navegación del selector de scope ─────────────────────────────────────
 
    public List<CompetitionDTOs.ScopeNodeDetail> getScopeChildren(Long groupId) {
        return groupRepo.findByParentId(groupId).stream()
            .filter(OrganizationalGroup::getActive)
            .map(g -> {
                boolean isLeaf = g.getGroupType() == GroupType.GRUPO
                    || !groupRepo.existsByParentIdAndActiveTrue(g.getId());
                int memberCount = isLeaf
                    ? (int) memberRepo.countByGroupIdAndActiveTrue(g.getId())
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
        return memberRepo.findActiveByGroupIdWithGroup(groupId).stream()
            .map(m -> {
                User u = m.getUser();
                return CompetitionDTOs.ScopeNodeDetail.builder()
                    .id(u.getId())
                    .name(u.getFirstName() + " " + u.getLastName())
                    .groupType("MEMBER")
                    .isLeaf(true)
                    .memberCount(0)
                    .children(List.of())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /** Crea un reto entre miembros del mismo grupo hoja. */
    @Transactional
    public CompetitionDTOs.Response createIntraGroupReto(
            Long groupId,
            CompetitionDTOs.CreateRequest req
    ) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        if (group.getGroupType() != GroupType.GRUPO) {
            throw new IllegalArgumentException(
                "Los retos entre miembros solo pueden crearse en un grupo hoja (tipo GRUPO)");
        }

        req.setScopeLevel(ScopeLevel.GRUPO);
        req.setScopeReferenceId(groupId);
        if (req.getParticipantGroupIds() != null && !req.getParticipantGroupIds().isEmpty()) {
            throw new IllegalArgumentException(
                "Un reto interno no admite grupos participantes; usa participantUserIds o deja vacío para todos los miembros");
        }
        return create(req);
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
 
    private void enrollOrganizationMembers(Competition competition, Long scopeRefId, List<Long> userIds) {
        List<User> users = (userIds != null && !userIds.isEmpty())
            ? userRepo.findAllById(userIds)
            : collectUsersUnderScope(scopeRefId);

        if (users.size() < 2) {
            throw new IllegalArgumentException(
                "Se requieren al menos 2 miembros en la organización para el ranking individual");
        }

        memberParticipantRepo.saveAll(users.stream()
            .map(u -> CompetitionMemberParticipant.builder()
                .competition(competition).user(u).score(0.0).build())
            .collect(Collectors.toList()));
    }

    private List<User> collectUsersUnderScope(Long scopeRefId) {
        List<User> result = new ArrayList<>();
        collectUsersRecursive(scopeRefId, result);
        return result.stream().distinct().toList();
    }

    private boolean isMemberCompetition(Competition c) {
        return c.getScopeLevel() == ScopeLevel.GRUPO
            || c.getParticipantMode() == ParticipantMode.ORGANIZATION_MEMBERS;
    }

    private void notifyRankChange(User user, String competitionName, Integer previousRank, int newRank) {
        if (previousRank == null || previousRank.equals(newRank)) return;
        if (newRank < previousRank) {
            notificationService.createNotification(
                    user,
                    NotificationType.SUCCESS,
                    NotificationPriority.HIGH,
                    "¡Subiste en el ranking!",
                    String.format("¡Subiste al %d.° lugar en '%s'! Sigue así.", newRank, competitionName),
                    "/competitions"
            );
        } else {
            notificationService.createNotification(
                    user,
                    NotificationType.INFO,
                    NotificationPriority.MEDIUM,
                    "Te adelantaron en el ranking",
                    String.format("Alguien te adelantó en '%s'. ¡Registra tu sesión de hoy!", competitionName),
                    "/competitions"
            );
        }
    }

    private void enrollMemberParticipants(Competition competition, Long groupId, List<Long> userIds) {
        OrganizationalGroup scopeGroup = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));

        List<User> users = (userIds != null && !userIds.isEmpty())
            ? userRepo.findAllById(userIds)
            : resolveActiveGroupMemberUsers(groupId);

        if (users.size() < 2) {
            throw new IllegalArgumentException(
                "Se requieren al menos 2 miembros activos en el grupo para un reto interno");
        }
        if (competition.getCompetitionType() == CompetitionType.VERSUS && users.size() != 2) {
            throw new IllegalArgumentException("VERSUS requiere exactamente 2 participantes");
        }

        validateUsersBelongToGroup(users, scopeGroup.getId());

        memberParticipantRepo.saveAll(users.stream()
            .map(u -> CompetitionMemberParticipant.builder()
                .competition(competition).user(u).score(0.0).build())
            .collect(Collectors.toList()));
    }

    private List<User> resolveActiveGroupMemberUsers(Long groupId) {
        return memberRepo.findActiveByGroupIdWithGroup(groupId).stream()
            .map(OrganizationalGroupMember::getUser)
            .distinct()
            .collect(Collectors.toList());
    }

    private void validateUsersBelongToGroup(List<User> users, Long groupId) {
        for (User user : users) {
            boolean belongs = memberRepo.existsByUserIdAndGroupIdAndActiveTrue(user.getId(), groupId)
                || (user.getPrimaryOrganizationalGroup() != null
                    && user.getPrimaryOrganizationalGroup().getId().equals(groupId));
            if (!belongs) {
                throw new IllegalArgumentException(
                    "El usuario " + user.getDisplayUsername() + " no pertenece al grupo seleccionado");
            }
        }
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
        LocalDate end   = effectiveEndDate(c);
 
        if (isMemberCompetition(c)) {
            List<CompetitionMemberParticipant> members = memberParticipantRepo.findLeaderboard(c.getId());
            Map<Long, Integer> previousRanks = members.stream()
                .filter(p -> p.getRank() != null)
                .collect(Collectors.toMap(p -> p.getUser().getId(), CompetitionMemberParticipant::getRank));
            int rank = 1;
            for (CompetitionMemberParticipant p : members) {
                Double score = activityRepo.sumUserScore(p.getUser().getId(), c.getMetricType(), start, end);
                p.setScore(score != null ? score : 0.0);
                p.setRank(rank);
                notifyRankChange(p.getUser(), c.getName(), previousRanks.get(p.getUser().getId()), rank);
                rank++;
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
        leaderboardCacheEvictor.evictForCompetition(c.getId());
    }
 
    private void requireManageCompetition(Competition competition) {
        if (competition.getScopeReference() != null) {
            accessService.requireManage(competition.getScopeReference());
        }
    }

    private Competition findOrThrow(Long id) {
        return competitionRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Competencia no encontrada: " + id));
    }
 
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
 
    private void requireCompetitionView(Competition c, User user) {
        if (accessService.isGlobalAdmin(user)) return;
        if (c.getScopeReference() != null && accessService.canView(user, c.getScopeReference())) return;

        OrganizationalGroup root = accessService.getRoot(c.getScopeReference());
        if (root != null) {
            List<Long> treeIds = accessService.collectTreeGroupIds(root.getId());
            if (memberParticipantRepo.existsByCompetitionIdAndUserId(c.getId(), user.getId())) return;
            for (Long gid : treeIds) {
                if (participantRepo.existsByCompetitionIdAndGroupId(c.getId(), gid)) {
                    if (accessService.isMemberOfGroupTree(user, groupRepo.findById(gid).orElse(null))) return;
                }
            }
            for (var m : memberRepo.findByUserIdAndActiveTrue(user.getId())) {
                if (treeIds.contains(m.getGroup().getId())) return;
            }
        }
        throw new AccessDeniedException("No tienes acceso a este reto");
    }

    private void refreshScoresIfNeeded(Competition c) {
        autoFinishIfExpired(c);
        if (c.getStatus() == CompetitionStatus.ACTIVE && scoresNeedRefresh(c)) {
            recalculateScores(c);
            competitionRepo.save(c);
        }
    }

    private boolean scoresNeedRefresh(Competition c) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(SCORE_REFRESH_SECONDS);
        var latest = isMemberCompetition(c)
                ? memberParticipantRepo.findLatestCalculation(c.getId())
                : participantRepo.findLatestCalculation(c.getId());
        return latest.isEmpty() || latest.get().isBefore(threshold);
    }

    /** CHALLENGE (y cualquier reto con endDate) pasa a FINISHED al vencer el período. */
    private void autoFinishIfExpired(Competition c) {
        if (c.getStatus() != CompetitionStatus.ACTIVE) return;
        if (c.getEndDate() == null) return;
        if (!c.getEndDate().isBefore(LocalDate.now())) return;
        recalculateScores(c);
        c.finish();
        competitionRepo.save(c);
    }

    private LocalDate effectiveEndDate(Competition c) {
        if (c.getEndDate() == null) return LocalDate.now();
        if (c.getStatus() == CompetitionStatus.FINISHED) return c.getEndDate();
        return c.getEndDate().isBefore(LocalDate.now()) ? c.getEndDate() : LocalDate.now();
    }

    private List<CompetitionDTOs.InternalRankingEntry> buildInternalRankingEntries(
            Long participantGroupId, Competition c) {
        List<User> users = new ArrayList<>();
        collectUsersRecursive(participantGroupId, users);
        List<Long> userIds = users.stream().map(User::getId).distinct().toList();
        if (userIds.isEmpty()) return List.of();

        Map<Long, User> userById = new LinkedHashMap<>();
        for (User u : users) userById.putIfAbsent(u.getId(), u);

        LocalDate start = c.getStartDate();
        LocalDate end   = effectiveEndDate(c);

        AtomicInteger pos = new AtomicInteger(1);
        return activityRepo.rankUsersByMetric(userIds, c.getMetricType(), start, end).stream()
            .map(r -> {
                Long uid = (Long) r[0];
                User u = userById.get(uid);
                if (u == null) u = userRepo.findById(uid).orElse(null);
                String fullName = u != null ? u.getFirstName() + " " + u.getLastName() : "Usuario";
                return CompetitionDTOs.InternalRankingEntry.builder()
                    .position(pos.getAndIncrement())
                    .userId(uid)
                    .fullName(fullName)
                    .username(u != null ? u.getDisplayUsername() : null)
                    .profilePictureUrl(u != null ? u.getProfilePictureUrl() : null)
                    .score(r[1] != null ? ((Number) r[1]).doubleValue() : 0.0)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private void requireCompetitionVisibleFromGroup(Competition c, Long groupId) {
        List<Long> treeIds = accessService.collectTreeGroupIds(groupId);
        boolean visible = competitionRepo
            .findVisibleForOrganizationalScope(treeIds, List.of(c.getStatus()))
            .stream()
            .anyMatch(v -> v.getId().equals(c.getId()));
        if (!visible) {
            throw new AccessDeniedException("Este reto no está disponible para el grupo seleccionado");
        }
    }

    private CompetitionDTOs.RetoSummary toRetoSummary(Competition c) {
        CompetitionDTOs.WinnerInfo leader = null;
        if (isMemberCompetition(c)) {
            List<CompetitionDTOs.MemberLeaderboardEntry> lb = memberParticipantRepo.findLeaderboard(c.getId()).stream()
                .map(p -> CompetitionDTOs.MemberLeaderboardEntry.builder()
                    .userId(p.getUser().getId())
                    .fullName(p.getUser().getFirstName() + " " + p.getUser().getLastName())
                    .score(p.getScore())
                    .build())
                .toList();
            leader = determineMemberWinner(c, lb);
        } else {
            List<CompetitionDTOs.LeaderboardEntry> lb = participantRepo.findLeaderboard(c.getId()).stream()
                .map(p -> CompetitionDTOs.LeaderboardEntry.builder()
                    .groupId(p.getGroup().getId())
                    .groupName(p.getGroup().getName())
                    .groupScore(p.getGroupScore())
                    .build())
                .toList();
            leader = determineGroupWinner(c, lb);
        }

        int participantCount = countParticipants(c);

        return CompetitionDTOs.RetoSummary.builder()
            .id(c.getId())
            .name(c.getName())
            .competitionType(c.getCompetitionType())
            .scopeLevel(c.getScopeLevel())
            .metricType(c.getMetricType())
            .startDate(c.getStartDate())
            .endDate(c.getEndDate())
            .status(c.getStatus())
            .participantCount(participantCount)
            .isMemberCompetition(isMemberCompetition(c))
            .leader(leader)
            .metricLabel(metricLabel(c.getMetricType()))
            .build();
    }

    private CompetitionDTOs.WinnerInfo determineGroupWinner(
            Competition c, List<CompetitionDTOs.LeaderboardEntry> lb) {
        if (lb == null || lb.isEmpty()) return null;
        var sorted = lb.stream()
            .sorted((a, b) -> Double.compare(b.getGroupScore(), a.getGroupScore()))
            .toList();
        var top = sorted.get(0);
        if (top.getGroupScore() == null || top.getGroupScore() <= 0) return null;

        boolean tie = sorted.size() > 1
            && sorted.get(1).getGroupScore() != null
            && sorted.get(1).getGroupScore().equals(top.getGroupScore());

        if (c.getCompetitionType() == CompetitionType.VERSUS && sorted.size() >= 2 && !tie) {
            var winner = top.getGroupScore() >= sorted.get(1).getGroupScore() ? top : sorted.get(1);
            return CompetitionDTOs.WinnerInfo.builder()
                .id(winner.getGroupId())
                .name(winner.getGroupName())
                .score(winner.getGroupScore())
                .tie(false)
                .type("GROUP")
                .build();
        }

        return CompetitionDTOs.WinnerInfo.builder()
            .id(top.getGroupId())
            .name(top.getGroupName())
            .score(top.getGroupScore())
            .tie(tie)
            .type("GROUP")
            .build();
    }

    private CompetitionDTOs.WinnerInfo determineMemberWinner(
            Competition c, List<CompetitionDTOs.MemberLeaderboardEntry> lb) {
        if (lb == null || lb.isEmpty()) return null;
        var sorted = lb.stream()
            .sorted((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0,
                a.getScore() != null ? a.getScore() : 0))
            .toList();
        var top = sorted.get(0);
        if (top.getScore() == null || top.getScore() <= 0) return null;

        boolean tie = sorted.size() > 1
            && sorted.get(1).getScore() != null
            && sorted.get(1).getScore().equals(top.getScore());

        return CompetitionDTOs.WinnerInfo.builder()
            .id(top.getUserId())
            .name(top.getFullName())
            .score(top.getScore())
            .tie(tie)
            .type("MEMBER")
            .build();
    }

    private String metricLabel(MetricType metricType) {
        return switch (metricType) {
            case SESSIONS -> "Sesiones completadas";
            case ACTIVE_MINUTES -> "Minutos activos";
            case WORKOUTS_COUNT -> "Entrenamientos";
            case FREE_ACTIVITY_COUNT -> "Actividades libres";
            case FREE_ACTIVITY_KM -> "Kilómetros (actividad libre)";
            case VOLUME_TOTAL -> "Volumen total (kg)";
        };
    }

    private int countParticipants(Competition c) {
        return isMemberCompetition(c)
            ? (int) memberParticipantRepo.findLeaderboard(c.getId()).size()
            : (int) participantRepo.findLeaderboard(c.getId()).size();
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
            .participantCount(countParticipants(c))
            .isMemberCompetition(isMemberCompetition(c))
            .participantMode(c.getParticipantMode())
            .build();
    }
    
    
    
    
    @Transactional
    public void recalculateScoresManual(Long competitionId) {
        Competition c = findOrThrow(competitionId);
        requireManageCompetition(c);
        recalculateScores(c);
        competitionRepo.save(c);
    }
    
    @Transactional(readOnly = true)
    public List<CompetitionDTOs.RetoSummary> getRetosForOrganizationalGroup(Long groupId) {
        accessService.requireView(groupId);
        List<Long> treeIds = accessService.collectTreeGroupIds(groupId);
        List<Competition> list = competitionRepo.findVisibleForOrganizationalScope(
            treeIds, List.of(CompetitionStatus.ACTIVE, CompetitionStatus.FINISHED));
        return list.stream().map(this::toRetoSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompetitionDTOs.CompetitionDetailView getDetailForUser(Long competitionId, User user) {
        return getDetailForUser(competitionId, user, null);
    }

    @Transactional(readOnly = true)
    public CompetitionDTOs.CompetitionDetailView getDetailForUser(Long competitionId, User user, Long groupContextId) {
        Competition c = findOrThrow(competitionId);
        requireCompetitionView(c, user);
        if (groupContextId != null) {
            requireCompetitionVisibleFromGroup(c, groupContextId);
        }
        refreshScoresIfNeeded(c);

        CompetitionDTOs.CompetitionDetailView.CompetitionDetailViewBuilder builder =
            CompetitionDTOs.CompetitionDetailView.builder()
                .competition(toResponse(c))
                .metricLabel(metricLabel(c.getMetricType()));

        LocalDateTime lastCalc = null;

        if (isMemberCompetition(c)) {
            List<CompetitionDTOs.MemberLeaderboardEntry> memberLb = getMemberLeaderboard(competitionId, user);
            builder.memberLeaderboard(memberLb);
            builder.winner(determineMemberWinner(c, memberLb));
            lastCalc = memberParticipantRepo.findLeaderboard(competitionId).stream()
                .map(CompetitionMemberParticipant::getLastCalculatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        } else {
            List<CompetitionDTOs.LeaderboardEntry> groupLb = getLeaderboard(competitionId, user);
            builder.groupLeaderboard(groupLb);
            builder.winner(determineGroupWinner(c, groupLb));
            lastCalc = participantRepo.findLeaderboard(competitionId).stream()
                .map(CompetitionParticipant::getLastCalculatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
            try {
                builder.internalRanking(getInternalRanking(competitionId, user));
            } catch (Exception ignored) {
                builder.internalRanking(List.of());
            }
        }

        try {
            builder.myScore(getMyScore(competitionId, user));
        } catch (Exception ignored) {
            builder.myScore(null);
        }

        return builder.lastCalculatedAt(lastCalc).build();
    }

    @Transactional(readOnly = true)
    public List<CompetitionDTOs.Response> getAllForUser(User user) {
        User fullUser = userRepo.findById(user.getId()).orElse(user);
        Map<Long, Competition> byId = new LinkedHashMap<>();

        if (fullUser.getPrimaryOrganizationalGroup() != null) {
            List<Long> ancestorIds = new ArrayList<>();
            var current = fullUser.getPrimaryOrganizationalGroup();
            int maxDepth = 5;
            while (current != null && maxDepth-- > 0) {
                ancestorIds.add(current.getId());
                current = current.getParent();
            }
            competitionRepo.findAllByParticipantGroupIds(ancestorIds)
                .forEach(c -> byId.put(c.getId(), c));
        }

        competitionRepo.findAllMemberCompetitionsForUser(fullUser.getId())
            .forEach(c -> byId.put(c.getId(), c));

        return byId.values().stream().map(this::toResponse).collect(Collectors.toList());
    }
    
    
    
}