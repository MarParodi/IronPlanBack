package com.example.ironplan.rest;

import com.example.ironplan.rest.dto.ForgotPasswordRequest;
import com.example.ironplan.rest.dto.ResetPasswordRequest;
import com.example.ironplan.rest.dto.VerifyResetCodeRequest;
import com.example.ironplan.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // POST /auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            passwordResetService.sendResetCode(req.getEmail());
            return ResponseEntity.ok(Map.of("message", "Código enviado al correo"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // POST /auth/verify-reset-code
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyResetCodeRequest req) {
        try {
            passwordResetService.verifyCode(req.getEmail(), req.getCode());
            return ResponseEntity.ok(Map.of("message", "Código válido"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // POST /auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            passwordResetService.resetPassword(req.getEmail(), req.getCode(), req.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}