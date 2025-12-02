// src/main/java/com/example/ironplan/repository/UserXpEventRepository.java
package com.example.ironplan.repository;

import com.example.ironplan.model.UserXpEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserXpEventRepository extends JpaRepository<UserXpEvent, Long> {
    long countByUser_Id(Long userId);
}
