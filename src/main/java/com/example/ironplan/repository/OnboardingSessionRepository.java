package com.example.ironplan.repository;

import com.example.ironplan.model.OnboardingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingSessionRepository extends JpaRepository<OnboardingSession, String> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<OnboardingSession> findByToken(String token);
}

