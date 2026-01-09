package com.example.ironplan.repository;

import com.example.ironplan.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    
    // Verificar si el usuario ya tiene una hazaña específica
    boolean existsByUser_IdAndAchievement_Code(Long userId, String achievementCode);
    
    // Obtener todas las hazañas de un usuario
    List<UserAchievement> findByUser_IdOrderByUnlockedAtDesc(Long userId);
    
    // Obtener hazañas no vistas de un usuario
    List<UserAchievement> findByUser_IdAndSeenFalseOrderByUnlockedAtDesc(Long userId);
    
    // Obtener una hazaña específica del usuario
    Optional<UserAchievement> findByUser_IdAndAchievement_Code(Long userId, String achievementCode);
    
    // Contar hazañas desbloqueadas por usuario
    long countByUser_Id(Long userId);
    
    // Obtener los códigos de hazañas desbloqueadas por un usuario
    @Query("SELECT ua.achievement.code FROM UserAchievement ua WHERE ua.user.id = :userId")
    Set<String> findUnlockedAchievementCodesByUserId(@Param("userId") Long userId);
}
