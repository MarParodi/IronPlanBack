package com.example.ironplan.repository;

import com.example.ironplan.model.FreeActivitySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreeActivitySessionRepository extends JpaRepository<FreeActivitySession, Long> {

    List<FreeActivitySession> findByUser_IdOrderByCompletedAtDesc(Long userId);
}
