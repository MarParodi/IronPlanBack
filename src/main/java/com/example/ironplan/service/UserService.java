package com.example.ironplan.service;

import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.UserUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User updateProfile(Long userId, UserUpdateDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // =====================
        // 🔹 CAMPOS NORMALES
        // =====================
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }

        if (dto.getLevel() != null) {
            user.setLevel(dto.getLevel());
        }

        if (dto.getTrainDays() != null) {
            user.setTrainDays(dto.getTrainDays());
        }

        if (dto.getWeight() != null) {
            user.setWeight(dto.getWeight());
        }

        if (dto.getHeight() != null) {
            user.setHeight(dto.getHeight());
        }

        // =====================
        // 🔐 PASSWORD (AISLADO)
        // =====================
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {

            String stored = user.getPassword();

            if (stored == null || stored.isBlank()) {
                // set inicial
                user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            } else {
                if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
                    throw new RuntimeException("CURRENT_PASSWORD_REQUIRED");
                }

                if (!passwordEncoder.matches(dto.getCurrentPassword(), stored)) {
                    throw new RuntimeException("CURRENT_PASSWORD_INVALID");
                }

                user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            }
        }

        // ⚠️ CLAVE: si NO hay newPassword → NO tocar password

        return userRepository.save(user);
    }

}
