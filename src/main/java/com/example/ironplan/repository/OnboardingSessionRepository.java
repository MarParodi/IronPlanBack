package com.example.ironplan.repository;

import com.example.ironplan.model.OnboardingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingSessionRepository extends JpaRepository<OnboardingSession, String> {
    Optional<OnboardingSession> findByToken(String token);
    Optional<OnboardingSession> findFirstByEmailOrderByCreatedAtDesc(String email);
    Optional<OnboardingSession> findFirstByUsernameOrderByCreatedAtDesc(String username);
}

