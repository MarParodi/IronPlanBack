// src/main/java/com/example/ironplan/repository/UserXpEventRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.UserXpEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserXpEventRepository extends JpaRepository<UserXpEvent, Long> {
    long countByUser_Id(Long userId);

    List<UserXpEvent> findByUser_IdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
