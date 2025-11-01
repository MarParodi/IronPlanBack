package com.example.ironplan.rest;

import com.example.ironplan.rest.dto.AuthReq;
import com.example.ironplan.rest.dto.AuthResp;
import com.example.ironplan.rest.dto.RegisterReq;
import com.example.ironplan.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // -------- REGISTRO --------
    @PostMapping("/register")
    public ResponseEntity<AuthResp> register(@Valid @RequestBody RegisterReq request) {
        return ResponseEntity.ok(authService.register(request));
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
}
