package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.CreateFreeActivityRequest;
import com.example.ironplan.rest.dto.FreeActivityResponse;
import com.example.ironplan.service.FreeActivityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/free-activities")
public class FreeActivityController {

    private final FreeActivityService freeActivityService;

    public FreeActivityController(FreeActivityService freeActivityService) {
        this.freeActivityService = freeActivityService;
    }

    @PostMapping
    public ResponseEntity<FreeActivityResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateFreeActivityRequest request
    ) {
        return ResponseEntity.ok(freeActivityService.create(user, request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FreeActivityResponse>> listMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(freeActivityService.listMine(user));
    }
}
