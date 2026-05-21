package com.example.ironplan.service;
 
import com.example.ironplan.rest.dto.InvitationDTOs;
import com.example.ironplan.rest.dto.JoinOrganizationDTOs;
import com.example.ironplan.model.GroupMembershipRole;
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.OrganizationalInvitation;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.OrganizationalGroupRepository;
import com.example.ironplan.repository.OrganizationalInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class OrganizationalInvitationService {
 
    private final OrganizationalInvitationRepository invitationRepo;
    private final OrganizationalGroupRepository groupRepo;
    private final OrganizationalAccessService accessService;
 
    // ─── Listar ───────────────────────────────────────────────
 
    public List<InvitationDTOs.Response> getAll(Long groupId, Boolean active) {
        List<OrganizationalInvitation> list;
 
        if (groupId != null && active != null) {
            list = invitationRepo.findByGroupIdAndActive(groupId, active);
        } else if (groupId != null) {
            list = invitationRepo.findByGroupId(groupId);
        } else if (active != null) {
            list = invitationRepo.findByActive(active);
        } else {
            list = invitationRepo.findAll();
        }
 
        User user = accessService.getCurrentUser();
        return list.stream()
            .filter(inv -> accessService.canView(user, inv.getGroup()))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
 
    // ─── Crear invitación para un grupo ───────────────────────
 
    @Transactional
    public InvitationDTOs.Response create(Long groupId, InvitationDTOs.CreateRequest req) {
        OrganizationalGroup group = groupRepo.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Grupo no encontrado: " + groupId));
 
        accessService.requireManage(group);

        if (!group.getActive()) {
            throw new IllegalArgumentException("No se puede crear una invitación para un grupo inactivo");
        }
 
        // Código: usar el provisto o generar uno legible
        String code = (req.getCode() != null && !req.getCode().isBlank())
            ? req.getCode().toUpperCase()
            : generateCode();
 
        if (invitationRepo.existsByCode(code)) {
            throw new IllegalArgumentException("El código '" + code + "' ya existe");
        }
 
        User creator = getCurrentUser();
 
        GroupMembershipRole role = req.getMembershipRole() != null
            ? req.getMembershipRole()
            : GroupMembershipRole.MEMBER;

        OrganizationalInvitation invitation = OrganizationalInvitation.builder()
            .code(code)
            .group(group)
            .maxUses(req.getMaxUses())
            .expiresAt(req.getExpiresAt())
            .usesCount(0)
            .active(true)
            .createdBy(creator)
            .membershipRole(role)
            .build();
 
        return toResponse(invitationRepo.save(invitation));
    }
 
    // ─── Desactivar ───────────────────────────────────────────
 
    @Transactional
    public void deactivate(Long id) {
        OrganizationalInvitation inv = findOrThrow(id);
        accessService.requireManage(inv.getGroup());
        inv.setActive(false);
        invitationRepo.save(inv);
    }
 
    // ─── Vista previa sin consumir el código ────────────────────

    @Transactional(readOnly = true)
    public InvitationUseResult peekCode(String code) {
        OrganizationalInvitation inv = findValidInvitation(normalizeCode(code));
        return new InvitationUseResult(inv.getGroup(), inv.getMembershipRole());
    }

    @Transactional(readOnly = true)
    public JoinOrganizationDTOs.CodePreviewResponse previewCode(String code) {
        OrganizationalInvitation inv = findValidInvitation(normalizeCode(code));
        OrganizationalGroup group = inv.getGroup();
        OrganizationalGroup root = accessService.getRoot(group);

        return JoinOrganizationDTOs.CodePreviewResponse.builder()
            .code(inv.getCode())
            .groupId(group.getId())
            .groupName(group.getName())
            .organizationRootName(root != null ? root.getName() : group.getName())
            .membershipRole(inv.getMembershipRole().name())
            .build();
    }

    // ─── Validar y usar un código (registro o unión posterior) ─

    @Transactional
    public OrganizationalGroup validateAndUse(String code) {
        return validateAndUseWithRole(code).group();
    }

    @Transactional
    public InvitationUseResult validateAndUseWithRole(String code) {
        OrganizationalInvitation inv = findValidInvitation(normalizeCode(code));
        inv.registerUse();
        invitationRepo.save(inv);
        return new InvitationUseResult(inv.getGroup(), inv.getMembershipRole());
    }

    public record InvitationUseResult(OrganizationalGroup group, GroupMembershipRole role) {}

    private OrganizationalInvitation findValidInvitation(String code) {
        OrganizationalInvitation inv = invitationRepo.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Código de invitación inválido"));

        if (!inv.isValid()) {
            throw new IllegalArgumentException("El código ha expirado o ya no está disponible");
        }
        if (!inv.getGroup().getActive()) {
            throw new IllegalArgumentException("El grupo de la invitación no está activo");
        }
        return inv;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        return code.trim().toUpperCase();
    }
 
    // ─── Helpers ──────────────────────────────────────────────
 
    private OrganizationalInvitation findOrThrow(Long id) {
        return invitationRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Invitación no encontrada: " + id));
    }
 
    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
 
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
 
    private InvitationDTOs.Response toResponse(OrganizationalInvitation inv) {
        return InvitationDTOs.Response.builder()
            .id(inv.getId())
            .code(inv.getCode())
            .groupId(inv.getGroup().getId())
            .groupName(inv.getGroup().getName())
            .maxUses(inv.getMaxUses())
            .usesCount(inv.getUsesCount())
            .expiresAt(inv.getExpiresAt())
            .active(inv.getActive())
            .membershipRole(inv.getMembershipRole())
            .build();
    }
}