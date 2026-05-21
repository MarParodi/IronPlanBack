package com.example.ironplan.rest;
 
import com.example.ironplan.rest.dto.GroupDTOs;
import com.example.ironplan.model.GroupType;
import com.example.ironplan.service.OrganizationalGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.ironplan.rest.dto.GroupTreeDTO;

import com.example.ironplan.rest.dto.OrganizationDTOs;
import com.example.ironplan.service.OrganizationCascadeService;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/admin/organizational-groups")
@RequiredArgsConstructor
public class OrganizationalGroupController {
 
    private final OrganizationalGroupService groupService;
    private final OrganizationCascadeService cascadeService;
 
    @GetMapping
    public ResponseEntity<List<GroupDTOs.Response>> getAll(
        @RequestParam(required = false) GroupType type,
        @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(groupService.getAll(type, active));
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<GroupDTOs.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getById(id));
    }
    
    @GetMapping("/tree")
    public ResponseEntity<List<GroupTreeDTO>> getTree() {
        return ResponseEntity.ok(groupService.getTree());
    }
 
    @PostMapping
    public ResponseEntity<GroupDTOs.Response> create(@Valid @RequestBody GroupDTOs.CreateRequest req) {
        return ResponseEntity.ok(groupService.create(req));
    }
 
    @PatchMapping("/{id}")
    public ResponseEntity<GroupDTOs.Response> update(
        @PathVariable Long id,
        @RequestBody GroupDTOs.UpdateRequest req
    ) {
        return ResponseEntity.ok(groupService.update(id, req));
    }
 
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        groupService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
 
    // Crear invitación para un grupo específico
    @PostMapping("/{id}/invitations")
    public ResponseEntity<?> createInvitation(
        @PathVariable Long id,
        @RequestBody com.example.ironplan.rest.dto.InvitationDTOs.CreateRequest req
    ) {
        return ResponseEntity.ok(
            invitationService().create(id, req)
        );
    }
    
    @PostMapping("/cascade")
    public ResponseEntity<OrganizationDTOs.NodeResponse> createCascade(
        @Valid @RequestBody OrganizationDTOs.CreateRequest req
    ) {
        return ResponseEntity.ok(cascadeService.createOrganization(req));
    }
     
    // Editar organización completa en cascada
    @PutMapping("/cascade/{id}")
    public ResponseEntity<OrganizationDTOs.NodeResponse> updateCascade(
        @PathVariable Long id,
        @Valid @RequestBody OrganizationDTOs.UpdateRequest req
    ) {
        return ResponseEntity.ok(cascadeService.updateOrganization(id, req));
    }
     
    // Obtener org completa con toda su jerarquía (para prellenar el modal de edición)
    @GetMapping("/cascade/{id}")
    public ResponseEntity<OrganizationDTOs.NodeResponse> getCascade(@PathVariable Long id) {
        return ResponseEntity.ok(cascadeService.getOrganization(id));
    }
    
    
 
    // Inyectamos vía método para evitar dependencia circular
    private com.example.ironplan.service.OrganizationalInvitationService invitationService() {
        return applicationContext.getBean(
            com.example.ironplan.service.OrganizationalInvitationService.class);
    }
 
    private final org.springframework.context.ApplicationContext applicationContext;
}
 
