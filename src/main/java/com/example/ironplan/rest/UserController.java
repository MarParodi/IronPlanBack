package com.example.ironplan.rest;

import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.MeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.getBirthday(),
                user.getXpPoints(),
                user.getLevel(),
                user.getTrainDays(),
                user.getGender(),
                user.getCreatedAt()
        );
    }
}
