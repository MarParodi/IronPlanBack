package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.MeResponse;
import com.example.ironplan.rest.dto.UserResponseDto;
import com.example.ironplan.rest.dto.UserUpdateDTO;
import com.example.ironplan.service.CloudinaryService;
import com.example.ironplan.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    // Inyección por constructor (Práctica recomendada)
    public UserController(UserRepository userRepository,
                          UserService userService,
                          CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayUsername(), // Usamos el username real
                user.getRole(),
                user.getBirthday(),
                user.getXpPoints(),
                user.getLevel(),
                user.getTrainDays(),
                user.getGender(),
                user.getCreatedAt(),
                user.getProfilePictureUrl(),
                user.getWeight(),
                user.getHeight()
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

