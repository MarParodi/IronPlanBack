package com.example.ironplan.rest;

import com.example.ironplan.rest.dto.AuthReq;
import com.example.ironplan.rest.dto.AuthResp;
import com.example.ironplan.rest.dto.RegisterReq;
import com.example.ironplan.rest.dto.RegisterStep1Req;
import com.example.ironplan.rest.dto.RegisterStep1Resp;
import com.example.ironplan.rest.dto.RegisterStep2Req;
import com.example.ironplan.rest.dto.RegisterStep3Req;
import com.example.ironplan.rest.dto.RegisterStep4Req;
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
}
