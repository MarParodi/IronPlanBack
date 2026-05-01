package com.example.ironplan.rest;

import com.example.ironplan.rest.dto.AuthReq;
import com.example.ironplan.rest.dto.AuthResp;
import com.example.ironplan.rest.dto.ForgotPasswordRequest;
import com.example.ironplan.rest.dto.RegisterReq;
import com.example.ironplan.rest.dto.RegisterStep1Req;
import com.example.ironplan.rest.dto.RegisterStep1Resp;
import com.example.ironplan.rest.dto.RegisterStep2Req;
import com.example.ironplan.rest.dto.RegisterStep3Req;
import com.example.ironplan.rest.dto.RegisterStep4Req;
import com.example.ironplan.rest.dto.ResetPasswordRequest;
import com.example.ironplan.rest.dto.VerifyResetCodeRequest;
import com.example.ironplan.service.AuthService;
import com.example.ironplan.service.PasswordResetService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    // -------- REGISTRO --------
    @PostMapping("/register")
    public ResponseEntity<AuthResp> register(@Valid @RequestBody RegisterReq request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // -------- REGISTRO (ONBOARDING MULTI-PASO) --------
    @PostMapping("/register/step1")
    public ResponseEntity<RegisterStep1Resp> registerStep1(@Valid @RequestBody RegisterStep1Req request) {
        return ResponseEntity.ok(authService.registerStep1(request));
    }

    @PostMapping("/register/step2")
    public ResponseEntity<Void> registerStep2(@Valid @RequestBody RegisterStep2Req request) {
        authService.registerStep2(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/step3")
    public ResponseEntity<Void> registerStep3(@Valid @RequestBody RegisterStep3Req request) {
        authService.registerStep3(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/step4")
    public ResponseEntity<AuthResp> registerStep4(@Valid @RequestBody RegisterStep4Req request) {
        return ResponseEntity.ok(authService.registerStep4(request));
    }

    // -------- LOGIN --------
    @PostMapping("/login")
    public ResponseEntity<AuthResp> login(@Valid @RequestBody AuthReq request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // -------- TEST (opcional) --------
    @GetMapping("/test")
    public String test() {
        return "AuthController funcionando correctamente 🚀";
    }
    
    //Nuevo
    
    
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
