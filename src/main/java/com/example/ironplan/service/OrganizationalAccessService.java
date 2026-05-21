package com.example.ironplan.service;

import com.example.ironplan.model.GroupMembershipRole;
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.OrganizationalGroupMember;
import com.example.ironplan.model.Role;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.OrganizationalGroupMemberRepository;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationalAccessService {

    private final OrganizationalGroupRepository groupRepo;
    private final OrganizationalGroupMemberRepository memberRepo;

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean isGlobalAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public OrganizationalGroup getRoot(OrganizationalGroup group) {
        if (group == null) return null;
        OrganizationalGroup current = group;
        int guard = 10;
        while (current.getParent() != null && guard-- > 0) {
            current = current.getParent();
        }
        return current;
    }

    public boolean isOrganizationCreator(User user, OrganizationalGroup group) {
        if (user == null || group == null) return false;
        OrganizationalGroup root = getRoot(group);
        if (root == null || root.getCreatedBy() == null) return false;
        return root.getCreatedBy().getId().equals(user.getId());
    }

    public boolean hasCreatedOrganization(User user) {
        if (user == null) return false;
        return groupRepo.existsByCreatedBy_IdAndParentIsNull(user.getId());
    }

    public boolean hasAnyMembership(User user) {
        if (user == null) return false;
        return !memberRepo.findByUserIdAndActiveTrue(user.getId()).isEmpty();
    }

    public boolean hasAdminMembership(User user) {
        if (user == null) return false;
        if (isGlobalAdmin(user) || hasCreatedOrganization(user)) return true;
        return !memberRepo.findByUserIdAndRoleAndActiveTrue(user.getId(), GroupMembershipRole.ADMIN).isEmpty();
    }

    public boolean isMemberOfGroupTree(User user, OrganizationalGroup target) {
        if (user == null || target == null) return false;
        OrganizationalGroup targetRoot = getRoot(target);
        if (targetRoot == null) return false;

        for (OrganizationalGroupMember m : memberRepo.findByUserIdAndActiveTrue(user.getId())) {
            OrganizationalGroup memberRoot = getRoot(m.getGroup());
            if (memberRoot != null && memberRoot.getId().equals(targetRoot.getId())) {
                if (isSameOrRelated(m.getGroup(), target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSameOrRelated(OrganizationalGroup memberGroup, OrganizationalGroup target) {
        if (memberGroup.getId().equals(target.getId())) return true;
        if (isDescendantOf(memberGroup, target.getId())) return true;
        if (isDescendantOf(target, memberGroup.getId())) return true;
        return false;
    }

    private boolean isDescendantOf(OrganizationalGroup node, Long ancestorId) {
        OrganizationalGroup current = node;
        int guard = 10;
        while (current != null && guard-- > 0) {
            if (current.getId().equals(ancestorId)) return true;
            current = current.getParent();
        }
        return false;
    }

    public boolean canView(User user, OrganizationalGroup group) {
        if (isGlobalAdmin(user)) return true;
        if (isOrganizationCreator(user, group)) return true;
        return isMemberOfGroupTree(user, group);
    }

    public boolean canManage(User user, OrganizationalGroup group) {
        return canManageGroup(user, group.getId());
    }

    public boolean canManageGroup(User user, Long groupId) {
        if (isGlobalAdmin(user)) return true;
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        if (isOrganizationCreator(user, group)) return true;

        if (memberRepo.findByUserIdAndGroupIdAndActiveTrue(user.getId(), groupId)
            .map(m -> m.getRole() == GroupMembershipRole.ADMIN)
            .orElse(false)) {
            return true;
        }

        OrganizationalGroup root = getRoot(group);
        if (root == null) return false;
        List<Long> treeIds = collectTreeGroupIds(root.getId());
        return memberRepo.findByUserIdAndRoleAndActiveTrue(user.getId(), GroupMembershipRole.ADMIN).stream()
            .anyMatch(m -> treeIds.contains(m.getGroup().getId()));
    }

    public boolean canManageOrganization(User user) {
        return hasAdminMembership(user);
    }

    public boolean hasOrganizationAccess(User user) {
        if (isGlobalAdmin(user)) return true;
        if (hasAnyMembership(user)) return true;
        return hasCreatedOrganization(user);
    }

    public void requireView(OrganizationalGroup group) {
        User user = getCurrentUser();
        if (!canView(user, group)) {
            throw new AccessDeniedException("No tienes acceso a este recurso organizacional");
        }
    }

    public void requireManage(OrganizationalGroup group) {
        User user = getCurrentUser();
        if (!canManage(user, group)) {
            throw new AccessDeniedException("No tienes permisos de administrador en este grupo");
        }
    }

    public void requireView(Long groupId) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        requireView(group);
    }

    public void requireManage(Long groupId) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
        requireManage(group);
    }

    public List<Long> collectTreeGroupIds(Long rootId) {
        List<Long> ids = new ArrayList<>();
        collectTreeIdsRecursive(rootId, ids);
        return ids;
    }

    private void collectTreeIdsRecursive(Long groupId, List<Long> ids) {
        ids.add(groupId);
        groupRepo.findByParentId(groupId).forEach(child -> collectTreeIdsRecursive(child.getId(), ids));
    }
}
