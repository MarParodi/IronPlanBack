package com.example.ironplan.rest;

import org.springframework.transaction.annotation.Transactional;

import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.rest.dto.JoinOrganizationDTOs;
import com.example.ironplan.rest.dto.MeResponse;
import com.example.ironplan.rest.dto.UserResponseDto;
import com.example.ironplan.rest.dto.UserUpdateDTO;
import com.example.ironplan.service.CloudinaryService;
import com.example.ironplan.service.OrganizationalAccessService;
import com.example.ironplan.model.GroupMembershipRole;
import com.example.ironplan.service.GroupMembershipService;
import com.example.ironplan.service.OrganizationalInvitationService;
import com.example.ironplan.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final OrganizationalAccessService accessService;
    private final OrganizationalInvitationService invitationService;
    private final GroupMembershipService membershipService;

    // Inyección por constructor (Práctica recomendada)
    public UserController(UserRepository userRepository,
                          UserService userService,
                          CloudinaryService cloudinaryService,
                          OrganizationalAccessService accessService,
                          OrganizationalInvitationService invitationService,
                          GroupMembershipService membershipService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
        this.accessService = accessService;
        this.invitationService = invitationService;
        this.membershipService = membershipService;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal User user) {
        User fullUser = userRepository.findById(user.getId()).orElse(user);
        return buildMeResponse(fullUser);
    }

    @PostMapping("/me/organization/validate-code")
    @Transactional(readOnly = true)
    public ResponseEntity<JoinOrganizationDTOs.CodePreviewResponse> validateOrganizationCode(
            @Valid @RequestBody JoinOrganizationDTOs.Request req
    ) {
        return ResponseEntity.ok(invitationService.previewCode(req.getCode()));
    }

    @PostMapping("/me/organization/join")
    @Transactional
    public MeResponse joinOrganization(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JoinOrganizationDTOs.Request req
    ) {
        User fullUser = userRepository.findById(user.getId()).orElseThrow();
        var useResult = invitationService.validateAndUseWithRole(req.getCode());
        membershipService.joinGroup(fullUser, useResult.group(), useResult.role());
        fullUser = userRepository.findById(user.getId()).orElseThrow();
        fullUser.setOrganizationCode(req.getCode().trim().toUpperCase());
        userRepository.save(fullUser);

        return buildMeResponse(fullUser);
    }

    private MeResponse buildMeResponse(User fullUser) {
        var group = fullUser.getPrimaryOrganizationalGroup();

        List<Long> ancestorIds = new ArrayList<>();
        List<String> pathParts = new ArrayList<>();
        var current = group;
        int maxDepth = 5;
        while (current != null && maxDepth-- > 0) {
            ancestorIds.add(0, current.getId());
            pathParts.add(0, current.getName());
            current = current.getParent();
        }

        String rootName = null;
        String middlePath = null;

        if (!pathParts.isEmpty()) {
            rootName = pathParts.get(0);
            if (pathParts.size() > 2) {
                middlePath = String.join(" · ", pathParts.subList(1, pathParts.size() - 1));
            }
        }

        return new MeResponse(
                fullUser.getId(),
                fullUser.getEmail(),
                fullUser.getDisplayUsername(),
                fullUser.getRole(),
                fullUser.getBirthday(),
                fullUser.getXpPoints(),
                fullUser.getLevel(),
                fullUser.getTrainDays(),
                fullUser.getGender(),
                fullUser.getCreatedAt(),
                fullUser.getProfilePictureUrl(),
                fullUser.getWeight(),
                fullUser.getHeight(),
                group != null ? group.getId() : null,
                group != null ? group.getName() : null,
                ancestorIds,
                rootName,
                middlePath,
                accessService.canManageOrganization(fullUser),
                accessService.hasOrganizationAccess(fullUser)
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UserUpdateDTO updateData
    ) {
        // 👇 SOLO usamos el ID
        User updatedUser = userService.updateProfile(currentUser.getId(), updateData);

        UserResponseDto dto = new UserResponseDto(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getProfilePictureUrl(),
                updatedUser.getCurrentRoutine() != null
                        ? updatedUser.getCurrentRoutine().getName()
                        : null
        );

        return ResponseEntity.ok(dto);
    }



    @PostMapping("/profile/photo")
    public ResponseEntity<?> uploadPhoto(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        // 1. Subir a Cloudinary
        Map<?, ?> result = cloudinaryService.upload(file, "ironplan/profiles");

        // 2. Obtener URL
        String photoUrl = (String) result.get("secure_url");

        // 3. Guardar usando la INSTANCIA inyectada (en minúsculas)
        currentUser.setProfilePictureUrl(photoUrl);
        userRepository.save(currentUser);

        return ResponseEntity.ok(Map.of(
                "url", photoUrl,
                "message", "Foto actualizada correctamente"
        ));
    }
}

