package com.example.ironplan.rest;
 
import com.example.ironplan.rest.dto.InvitationDTOs;
import com.example.ironplan.service.OrganizationalInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/admin/organizational-invitations")
@RequiredArgsConstructor
public class OrganizationalInvitationController {
 
    private final OrganizationalInvitationService invitationService;
 
    @GetMapping
    public ResponseEntity<List<InvitationDTOs.Response>> getAll(
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(invitationService.getAll(groupId, active));
    }
 
    @PatchMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        invitationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}