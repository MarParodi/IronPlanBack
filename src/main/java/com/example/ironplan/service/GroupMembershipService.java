package com.example.ironplan.service;

import com.example.ironplan.model.GroupMembershipRole;
import com.example.ironplan.model.GroupType;
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.OrganizationalGroupMember;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.OrganizationalGroupMemberRepository;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.GrupoDTOs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class GroupMembershipService {

    private final OrganizationalGroupMemberRepository memberRepo;
    private final OrganizationalGroupRepository groupRepo;
    private final UserRepository userRepo;
    private final OrganizationalAccessService accessService;
    private final TransactionTemplate transactionTemplate;

    @PostConstruct
    public void migrateLegacyMemberships() {
        transactionTemplate.execute(status -> {
            List<User> users = userRepo.findAll();
            for (User user : users) {
                if (user.getPrimaryOrganizationalGroup() == null) continue;
                OrganizationalGroup group = user.getPrimaryOrganizationalGroup();
                if (memberRepo.existsByUserIdAndGroupIdAndActiveTrue(user.getId(), group.getId())) {
                    continue;
                }
                GroupMembershipRole role = accessService.isOrganizationCreator(user, group)
                    ? GroupMembershipRole.ADMIN
                    : GroupMembershipRole.MEMBER;
                memberRepo.save(OrganizationalGroupMember.builder()
                    .user(user)
                    .group(group)
                    .role(role)
                    .active(true)
                    .build());
            }
            return null;
        });
    }

    @Transactional
    public OrganizationalGroupMember joinGroup(User user, OrganizationalGroup group, GroupMembershipRole role) {
        OrganizationalGroup root = accessService.getRoot(group);
        if (root != null && hasActiveMembershipInRoot(user.getId(), root.getId())) {
            throw new IllegalArgumentException("Ya perteneces a esta organización. Solo puedes tener una membresía por organización.");
        }

        Optional<OrganizationalGroupMember> existing = memberRepo
            .findByUserIdAndGroupIdAndActiveTrue(user.getId(), group.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Ya perteneces a este grupo");
        }

        OrganizationalGroupMember membership = memberRepo.save(OrganizationalGroupMember.builder()
            .user(user)
            .group(group)
            .role(role != null ? role : GroupMembershipRole.MEMBER)
            .active(true)
            .build());

        User managed = userRepo.findById(user.getId()).orElseThrow();
        managed.setPrimaryOrganizationalGroup(group);
        managed.setOrganizationCode(managed.getOrganizationCode());
        userRepo.save(managed);

        return membership;
    }

    @Transactional
    public void ensureCreatorMembership(User creator, OrganizationalGroup root) {
        if (creator == null || root == null) return;
        if (memberRepo.existsByUserIdAndGroupIdAndActiveTrue(creator.getId(), root.getId())) {
            return;
        }
        memberRepo.save(OrganizationalGroupMember.builder()
            .user(creator)
            .group(root)
            .role(GroupMembershipRole.ADMIN)
            .active(true)
            .build());
        User managed = userRepo.findById(creator.getId()).orElseThrow();
        if (managed.getPrimaryOrganizationalGroup() == null) {
            managed.setPrimaryOrganizationalGroup(root);
            userRepo.save(managed);
        }
    }

    public boolean hasActiveMembershipInRoot(Long userId, Long rootId) {
        List<Long> treeIds = accessService.collectTreeGroupIds(rootId);
        return memberRepo.existsActiveMembershipInGroups(userId, treeIds);
    }

    public List<OrganizationalGroupMember> getActiveMemberships(Long userId) {
        return memberRepo.findByUserIdAndActiveTrue(userId);
    }

    public List<GrupoDTOs.MembershipSummary> listMisGrupos(User user) {
        return getActiveMemberships(user.getId()).stream()
            .map(m -> toMembershipSummary(user, m))
            .toList();
    }

    public List<GrupoDTOs.MembershipSummary> listAdministrarGrupos(User user) {
        if (accessService.isGlobalAdmin(user)) {
            return groupRepo.findByGroupTypeAndActiveTrue(GroupType.EMPRESA).stream()
                .map(g -> toAdminSummaryFromRoot(user, g))
                .toList();
        }

        List<GrupoDTOs.MembershipSummary> result = new ArrayList<>();
        for (OrganizationalGroupMember m : memberRepo.findByUserIdAndRoleAndActiveTrue(user.getId(), GroupMembershipRole.ADMIN)) {
            result.add(toMembershipSummary(user, m));
        }
        for (OrganizationalGroup root : groupRepo.findAll().stream()
            .filter(g -> g.getParent() == null && g.getCreatedBy() != null
                && g.getCreatedBy().getId().equals(user.getId()))
            .toList()) {
            if (result.stream().noneMatch(s -> s.getGroupId().equals(root.getId()))) {
                result.add(toAdminSummaryFromRoot(user, root));
            }
        }
        return result;
    }

    public Optional<OrganizationalGroupMember> getMembership(Long userId, Long groupId) {
        return memberRepo.findByUserIdAndGroupIdAndActiveTrue(userId, groupId);
    }

    public GroupMembershipRole resolveRoleForUser(User user, OrganizationalGroup group) {
        if (accessService.isGlobalAdmin(user)) return GroupMembershipRole.ADMIN;
        if (accessService.isOrganizationCreator(user, group)) return GroupMembershipRole.ADMIN;
        return getMembership(user.getId(), group.getId())
            .map(OrganizationalGroupMember::getRole)
            .orElse(null);
    }

    public int countMembersForScope(Long groupId) {
        List<Long> scopeIds = accessService.collectTreeGroupIds(groupId);
        return (int) memberRepo.findAll().stream()
            .filter(m -> m.getActive() && scopeIds.contains(m.getGroup().getId()))
            .count();
    }

    public List<GrupoDTOs.MemberItem> listGroupMembers(Long groupId, boolean includeDescendants) {
        if (!includeDescendants) {
            return memberRepo.findActiveByGroupIdWithGroup(groupId).stream()
                .map(this::toMemberItem)
                .toList();
        }
        List<Long> scopeIds = accessService.collectTreeGroupIds(groupId);
        if (scopeIds.isEmpty()) return List.of();
        return memberRepo.findActiveByGroupIdsWithGroup(scopeIds).stream()
            .map(this::toMemberItem)
            .toList();
    }

    public boolean canLeave(User user, Long groupId) {
        if (user == null || groupId == null) return false;
        Optional<OrganizationalGroupMember> membership = getMembership(user.getId(), groupId);
        if (membership.isEmpty()) return false;
        OrganizationalGroupMember m = membership.get();
        if (accessService.isOrganizationCreator(user, m.getGroup())) return false;
        if (m.getRole() == GroupMembershipRole.ADMIN
            && memberRepo.countByGroupIdAndActiveTrueAndRole(groupId, GroupMembershipRole.ADMIN) <= 1) {
            return false;
        }
        return true;
    }

    @Transactional
    public GrupoDTOs.MemberItem addMember(Long groupId, User actor, GrupoDTOs.AddMemberRequest req) {
        accessService.requireManage(groupId);
        if (req == null || req.getIdentifier() == null || req.getIdentifier().isBlank()) {
            throw new IllegalArgumentException("Indica el usuario (email o nombre de usuario)");
        }
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));

        String key = req.getIdentifier().trim();
        User target = userRepo.findByUsername(key)
            .or(() -> userRepo.findByEmail(key))
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + key));

        GroupMembershipRole role = parseRole(req.getMembershipRole());

        if (memberRepo.existsByUserIdAndGroupIdAndActiveTrue(target.getId(), groupId)) {
            throw new IllegalArgumentException("El usuario ya es miembro de este grupo");
        }

        OrganizationalGroup root = accessService.getRoot(group);
        if (root != null && hasActiveMembershipInRoot(target.getId(), root.getId())) {
            throw new IllegalArgumentException(
                "El usuario ya pertenece a esta organización en otro grupo. Retíralo allí antes de reasignarlo.");
        }

        Optional<OrganizationalGroupMember> inactive = memberRepo.findByUserIdAndGroupId(target.getId(), groupId);
        if (inactive.isPresent()) {
            OrganizationalGroupMember m = inactive.get();
            m.setActive(true);
            m.setRole(role);
            m.setJoinedAt(java.time.LocalDateTime.now());
            memberRepo.save(m);
            syncPrimaryGroup(target, group);
            return toMemberItem(m);
        }

        return toMemberItem(joinGroup(target, group, role));
    }

    @Transactional
    public void removeMember(Long groupId, Long targetUserId, User actor) {
        accessService.requireManage(groupId);
        OrganizationalGroupMember m = memberRepo.findByUserIdAndGroupIdAndActiveTrue(targetUserId, groupId)
            .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado en este grupo"));

        if (m.getRole() == GroupMembershipRole.ADMIN
            && memberRepo.countByGroupIdAndActiveTrueAndRole(groupId, GroupMembershipRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("No puedes eliminar al único administrador del grupo");
        }

        if (accessService.isOrganizationCreator(m.getUser(), m.getGroup())) {
            throw new IllegalArgumentException("No puedes dar de baja al creador de la organización");
        }

        deactivateMembership(m);
    }

    @Transactional
    public void leaveGroup(Long groupId, User actor) {
        OrganizationalGroupMember m = memberRepo.findByUserIdAndGroupIdAndActiveTrue(actor.getId(), groupId)
            .orElseThrow(() -> new IllegalArgumentException("No perteneces a este grupo"));

        if (accessService.isOrganizationCreator(actor, m.getGroup())) {
            throw new IllegalArgumentException("El creador de la organización no puede salir del grupo");
        }
        if (m.getRole() == GroupMembershipRole.ADMIN
            && memberRepo.countByGroupIdAndActiveTrueAndRole(groupId, GroupMembershipRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("No puedes salir siendo el único administrador del grupo");
        }

        deactivateMembership(m);
    }

    private void deactivateMembership(OrganizationalGroupMember m) {
        Long groupId = m.getGroup().getId();
        Long userId = m.getUser().getId();
        m.setActive(false);
        memberRepo.save(m);

        User target = userRepo.findById(userId).orElseThrow();
        if (target.getPrimaryOrganizationalGroup() != null
            && target.getPrimaryOrganizationalGroup().getId().equals(groupId)) {
            target.setPrimaryOrganizationalGroup(null);
            userRepo.save(target);
        }
    }

    @Transactional
    public GrupoDTOs.MemberItem updateMemberRole(Long groupId, Long targetUserId, GrupoDTOs.UpdateMemberRoleRequest req) {
        accessService.requireManage(groupId);
        if (req == null || req.getRole() == null || req.getRole().isBlank()) {
            throw new IllegalArgumentException("Indica el rol");
        }
        GroupMembershipRole newRole = parseRole(req.getRole());

        OrganizationalGroupMember m = memberRepo.findByUserIdAndGroupIdAndActiveTrue(targetUserId, groupId)
            .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado en este grupo"));

        if (m.getRole() == GroupMembershipRole.ADMIN && newRole == GroupMembershipRole.MEMBER
            && memberRepo.countByGroupIdAndActiveTrueAndRole(groupId, GroupMembershipRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("Debe haber al menos un administrador en el grupo");
        }

        m.setRole(newRole);
        return toMemberItem(memberRepo.save(m));
    }

    private GroupMembershipRole parseRole(String role) {
        if (role == null || role.isBlank()) return GroupMembershipRole.MEMBER;
        try {
            return GroupMembershipRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido. Usa MEMBER o ADMIN");
        }
    }

    private void syncPrimaryGroup(User user, OrganizationalGroup group) {
        User managed = userRepo.findById(user.getId()).orElseThrow();
        if (managed.getPrimaryOrganizationalGroup() == null) {
            managed.setPrimaryOrganizationalGroup(group);
            userRepo.save(managed);
        }
    }

    private GrupoDTOs.MemberItem toMemberItem(OrganizationalGroupMember m) {
        OrganizationalGroup g = m.getGroup();
        return GrupoDTOs.MemberItem.builder()
            .userId(m.getUser().getId())
            .fullName(m.getUser().getFirstName() + " " + m.getUser().getLastName())
            .username(m.getUser().getDisplayUsername())
            .role(m.getRole().name())
            .joinedAt(m.getJoinedAt())
            .profilePictureUrl(m.getUser().getProfilePictureUrl())
            .groupId(g.getId())
            .groupName(g.getName())
            .level(m.getUser().getLevel() != null ? m.getUser().getLevel().name() : null)
            .build();
    }

    private GrupoDTOs.MembershipSummary toMembershipSummary(User user, OrganizationalGroupMember m) {
        OrganizationalGroup group = m.getGroup();
        GrupoDTOs.HierarchyPath path = buildHierarchyPath(group);
        GroupMembershipRole role = resolveRoleForUser(user, group);
        return GrupoDTOs.MembershipSummary.builder()
            .groupId(group.getId())
            .groupName(group.getName())
            .groupCode(group.getCode())
            .groupType(group.getGroupType().name())
            .role(role != null ? role.name() : m.getRole().name())
            .canManage(accessService.canManageGroup(user, group.getId()))
            .canLeave(canLeave(user, group.getId()))
            .memberCount(countMembersInSubtree(group.getId()))
            .activeCompetitionsCount(0)
            .hierarchyPath(path)
            .build();
    }

    private GrupoDTOs.MembershipSummary toAdminSummaryFromRoot(User user, OrganizationalGroup root) {
        GrupoDTOs.HierarchyPath path = buildHierarchyPath(root);
        return GrupoDTOs.MembershipSummary.builder()
            .groupId(root.getId())
            .groupName(root.getName())
            .groupCode(root.getCode())
            .groupType(root.getGroupType().name())
            .role(GroupMembershipRole.ADMIN.name())
            .canManage(true)
            .canLeave(canLeave(user, root.getId()))
            .memberCount(countMembersInSubtree(root.getId()))
            .activeCompetitionsCount(0)
            .hierarchyPath(path)
            .build();
    }

    public GrupoDTOs.HierarchyPath buildHierarchyPath(OrganizationalGroup group) {
        List<String> parts = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        OrganizationalGroup current = group;
        int guard = 10;
        while (current != null && guard-- > 0) {
            parts.add(0, current.getName());
            ids.add(0, current.getId());
            current = current.getParent();
        }
        String rootName = parts.isEmpty() ? null : parts.get(0);
        String middlePath = null;
        if (parts.size() > 2) {
            middlePath = String.join(" · ", parts.subList(1, parts.size() - 1));
        }
        return GrupoDTOs.HierarchyPath.builder()
            .ancestorIds(ids)
            .rootName(rootName)
            .middlePath(middlePath)
            .leafName(parts.isEmpty() ? null : parts.get(parts.size() - 1))
            .displayPath(String.join(" · ", parts))
            .build();
    }

    private int countMembersInSubtree(Long groupId) {
        int count = (int) memberRepo.countByGroupIdAndActiveTrue(groupId);
        for (OrganizationalGroup child : groupRepo.findByParentId(groupId)) {
            count += countMembersInSubtree(child.getId());
        }
        return count;
    }

}
