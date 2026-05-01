package com.example.ironplan.service;
 
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.User;
import com.example.ironplan.model.GroupType;
import com.example.ironplan.model.OrganizationKind;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import com.example.ironplan.rest.dto.OrganizationDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class OrganizationCascadeService {
 
    private final OrganizationalGroupRepository groupRepo;
 
    // ─── Crear jerarquía completa en una sola transacción ────────────────────
 
    @Transactional
    public OrganizationDTOs.NodeResponse createOrganization(OrganizationDTOs.CreateRequest req) {
        User creator = getCurrentUser();
 
        // Nivel 1 — raíz
        OrganizationalGroup root = OrganizationalGroup.builder()
            .name(req.getName())
            .groupType(GroupType.EMPRESA)
            .organizationKind(req.getOrganizationKind())
            .code(generateCode(req.getName()))
            .active(true)
            .createdBy(creator)
            .build();
 
        groupRepo.save(root);
 
        // Crear hijos en cascada
        createChildren(root, req.getChildren(), creator, 2);
 
        return toNodeResponse(root);
    }
 
    // ─── Editar jerarquía completa ────────────────────────────────────────────
 
    @Transactional
    public OrganizationDTOs.NodeResponse updateOrganization(Long rootId, OrganizationDTOs.UpdateRequest req) {
        OrganizationalGroup root = findOrThrow(rootId);
 
        root.setName(req.getName());
        if (req.getOrganizationKind() != null) {
            root.setOrganizationKind(req.getOrganizationKind());
        }
        groupRepo.save(root);
 
        // Reconciliar hijos
        reconcileChildren(root, req.getChildren(), getCurrentUser(), 2);
 
        return toNodeResponse(findOrThrow(rootId));
    }
 
    // ─── Crear hijos recursivamente ───────────────────────────────────────────
 
    private void createChildren(OrganizationalGroup parent,
                                List<OrganizationDTOs.NodeRequest> childRequests,
                                User creator, int level) {
        if (childRequests == null || childRequests.isEmpty()) return;
        if (level > 4) return; // máximo 4 niveles
 
        GroupType type = levelToGroupType(level);
 
        for (OrganizationDTOs.NodeRequest childReq : childRequests) {
            OrganizationalGroup child = OrganizationalGroup.builder()
                .name(childReq.getName())
                .groupType(type)
                .parent(parent)
                .code(generateCode(childReq.getName()))
                .active(true)
                .createdBy(creator)
                .build();
 
            groupRepo.save(child);
            createChildren(child, childReq.getChildren(), creator, level + 1);
        }
    }
 
    // ─── Reconciliar hijos al editar ──────────────────────────────────────────
    // - Nodos con id existente → actualizar nombre
    // - Nodos sin id           → crear nuevos
    // - Nodos que ya existían pero no vienen → desactivar
 
    private void reconcileChildren(OrganizationalGroup parent,
                                   List<OrganizationDTOs.NodeRequest> incoming,
                                   User creator, int level) {
        if (level > 4) return;
 
        List<OrganizationalGroup> existing = groupRepo.findByParentId(parent.getId());
 
        // IDs que vienen en el request
        List<Long> incomingIds = incoming.stream()
            .filter(r -> r.getId() != null)
            .map(OrganizationDTOs.NodeRequest::getId)
            .collect(Collectors.toList());
 
        // Desactivar los que no vienen
        for (OrganizationalGroup ex : existing) {
            if (!incomingIds.contains(ex.getId())) {
                ex.setActive(false);
                groupRepo.save(ex);
            }
        }
 
        GroupType type = levelToGroupType(level);
 
        for (OrganizationDTOs.NodeRequest nodeReq : incoming) {
            if (nodeReq.getId() != null) {
                // Actualizar existente
                OrganizationalGroup node = findOrThrow(nodeReq.getId());
                node.setName(nodeReq.getName());
                groupRepo.save(node);
                reconcileChildren(node, nodeReq.getChildren(), creator, level + 1);
            } else {
                // Crear nuevo
                OrganizationalGroup node = OrganizationalGroup.builder()
                    .name(nodeReq.getName())
                    .groupType(type)
                    .parent(parent)
                    .code(generateCode(nodeReq.getName()))
                    .active(true)
                    .createdBy(creator)
                    .build();
                groupRepo.save(node);
                createChildren(node, nodeReq.getChildren(), creator, level + 1);
            }
        }
    }
 
    // ─── Helpers ──────────────────────────────────────────────────────────────
 
    private GroupType levelToGroupType(int level) {
        return switch (level) {
            case 2 -> GroupType.FACULTAD;
            case 3 -> GroupType.CARRERA;
            case 4 -> GroupType.GRUPO;
            default -> GroupType.GRUPO;
        };
    }
 
    private String generateCode(String name) {
        String base = name.toUpperCase()
            .replaceAll("[^A-Z0-9]", "-")
            .replaceAll("-+", "-")
            .substring(0, Math.min(name.length(), 15));
        return base + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
 
    private OrganizationalGroup findOrThrow(Long id) {
        return groupRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + id));
    }
 
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
 
    // ─── Convertir a response ─────────────────────────────────────────────────
 
    public OrganizationDTOs.NodeResponse toNodeResponse(OrganizationalGroup g) {
        List<OrganizationalGroup> children = groupRepo.findByParentId(g.getId())
            .stream()
            .filter(OrganizationalGroup::getActive)
            .collect(Collectors.toList());
 
        return OrganizationDTOs.NodeResponse.builder()
            .id(g.getId())
            .name(g.getName())
            .code(g.getCode())
            .groupType(g.getGroupType().name())
            .organizationKind(g.getOrganizationKind())
            .active(g.getActive())
            .children(children.stream()
                .map(this::toNodeResponse)
                .collect(Collectors.toList()))
            .build();
    }
    
    
    
    public OrganizationDTOs.NodeResponse getOrganization(Long id) {
        OrganizationalGroup group = groupRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Organización no encontrada: " + id));
        return toNodeResponse(group);
    }
}