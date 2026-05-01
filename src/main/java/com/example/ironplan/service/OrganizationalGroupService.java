package com.example.ironplan.service;
 
import com.example.ironplan.rest.dto.GroupDTOs;
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.User;
import com.example.ironplan.model.GroupType;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.ironplan.rest.dto.GroupTreeDTO;
import com.example.ironplan.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
 
@Service
@RequiredArgsConstructor
public class OrganizationalGroupService {
 
    private final OrganizationalGroupRepository groupRepo;
    private final UserRepository userRepo;
 
    // ─── Listar ───────────────────────────────────────────────
 
    public List<GroupDTOs.Response> getAll(GroupType type, Boolean active) {
        List<OrganizationalGroup> groups;
 
        if (type != null && active != null) {
            groups = groupRepo.findByGroupTypeAndActive(type, active);
        } else if (type != null) {
            groups = groupRepo.findByGroupType(type);
        } else if (active != null) {
            groups = groupRepo.findByActive(active);
        } else {
            groups = groupRepo.findAll();
        }
 
        return groups.stream().map(this::toResponse).collect(Collectors.toList());
    }
 
    public GroupDTOs.Response getById(Long id) {
        return toResponse(findOrThrow(id));
    }
 
    // ─── Crear ────────────────────────────────────────────────
 
    @Transactional
    public GroupDTOs.Response create(GroupDTOs.CreateRequest req) {
        // Validación: EMPRESA no tiene padre
        if (req.getGroupType() == GroupType.EMPRESA && req.getParentId() != null) {
            throw new IllegalArgumentException("Una EMPRESA no puede tener grupo padre");
        }
        // Validación: todos los demás necesitan padre
        if (req.getGroupType() != GroupType.EMPRESA && req.getParentId() == null) {
            throw new IllegalArgumentException("Este tipo de grupo requiere un grupo padre");
        }
        // Validación: GRUPO no puede tener hijos (se valida al crear hijos, pero aquí
        // verificamos que el padre no sea tipo GRUPO)
        if (req.getParentId() != null) {
            OrganizationalGroup parent = findOrThrow(req.getParentId());
            if (parent.getGroupType() == GroupType.GRUPO) {
                throw new IllegalArgumentException("Un GRUPO no puede tener subgrupos");
            }
        }
 
        // Código: usar el provisto o generar uno
        String code = (req.getCode() != null && !req.getCode().isBlank())
            ? req.getCode()
            : generateCode(req.getName());
 
        if (groupRepo.existsByCode(code)) {
            throw new IllegalArgumentException("El código '" + code + "' ya está en uso");
        }
 
        User creator = getCurrentUser();
 
        OrganizationalGroup group = OrganizationalGroup.builder()
            .name(req.getName())
            .groupType(req.getGroupType())
            .parent(req.getParentId() != null ? findOrThrow(req.getParentId()) : null)
            .code(code)
            .active(true)
            .createdBy(creator)
            .organizationKind(req.getOrganizationKind())
            .build();
 
        return toResponse(groupRepo.save(group));
    }
 
    // ─── Actualizar ───────────────────────────────────────────
 
    @Transactional
    public GroupDTOs.Response update(Long id, GroupDTOs.UpdateRequest req) {
        OrganizationalGroup group = findOrThrow(id);
 
        if (req.getName() != null)   group.setName(req.getName());
        if (req.getActive() != null) group.setActive(req.getActive());
        if (req.getParentId() != null) {
            group.setParent(findOrThrow(req.getParentId()));
        }
 
        return toResponse(groupRepo.save(group));
    }
 
    // ─── Baja lógica ──────────────────────────────────────────
 
    @Transactional
    public void deactivate(Long id) {
        OrganizationalGroup group = findOrThrow(id);
        group.setActive(false);
        groupRepo.save(group);
    }
 
    // ─── Helpers ──────────────────────────────────────────────
 
    private OrganizationalGroup findOrThrow(Long id) {
        return groupRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + id));
    }
 
    private String generateCode(String name) {
        String base = name.toUpperCase()
            .replaceAll("[^A-Z0-9]", "-")
            .replaceAll("-+", "-")
            .substring(0, Math.min(name.length(), 20));
        return base + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
 
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
 
    public GroupDTOs.Response toResponse(OrganizationalGroup g) {
        return GroupDTOs.Response.builder()
            .id(g.getId())
            .name(g.getName())
            .groupType(g.getGroupType())
            .parentId(g.getParent() != null ? g.getParent().getId() : null)
            .parentName(g.getParent() != null ? g.getParent().getName() : null)
            .code(g.getCode())
            .active(g.getActive())
            .createdAt(g.getCreatedAt())
            .organizationKind(g.getOrganizationKind())
            .build();
    }
    
    
    
    
 // ─── Árbol jerárquico ─────────────────────────────────────

    public List<GroupTreeDTO> getTree() {
        List<OrganizationalGroup> all = groupRepo.findAll();

        Map<Long, GroupTreeDTO> map = new LinkedHashMap<>();
        for (OrganizationalGroup g : all) {
            map.put(g.getId(), toTreeNode(g));
        }

        Map<Long, Integer> memberCounts = userRepo.countMembersByGroup();

        List<GroupTreeDTO> roots = new ArrayList<>();
        for (OrganizationalGroup g : all) {
            GroupTreeDTO node = map.get(g.getId());
            node.setTotalMembers(memberCounts.getOrDefault(g.getId(), 0));

            if (g.getParent() == null) {
                roots.add(node);
            } else {
                GroupTreeDTO parentNode = map.get(g.getParent().getId());
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                    parentNode.setTotalChildren(parentNode.getTotalChildren() + 1);
                }
            }
        }

        roots.forEach(this::propagateCounts);
        return roots;
    }

    private void propagateCounts(GroupTreeDTO node) {
        if (node.getChildren().isEmpty()) return;
        node.getChildren().forEach(this::propagateCounts);
        int sumMembers = node.getChildren().stream()
            .mapToInt(GroupTreeDTO::getTotalMembers).sum() + node.getTotalMembers();
        node.setTotalMembers(sumMembers);
    }

    private GroupTreeDTO toTreeNode(OrganizationalGroup g) {
        return GroupTreeDTO.builder()
            .id(g.getId())
            .name(g.getName())
            .code(g.getCode())
            .groupType(g.getGroupType())
            .organizationKind(g.getOrganizationKind())
            .active(g.getActive())
            .parentId(g.getParent() != null ? g.getParent().getId() : null)
            .parentName(g.getParent() != null ? g.getParent().getName() : null)
            .totalMembers(0)
            .totalChildren(0)
            .children(new ArrayList<>())
            .build();
    }
    
    
    
}