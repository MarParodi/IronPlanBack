package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.AchievementDto;
import com.example.ironplan.rest.dto.UserAchievementDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepo;
    private final UserAchievementRepository userAchievementRepo;
    private final WorkoutSessionRepository workoutSessionRepo;
    private final RoutineTemplateRepository routineTemplateRepo;
    private final XpService xpService;

    // Códigos de hazañas
    public static final String FIRST_WORKOUT = "FIRST_WORKOUT";
    public static final String TEN_WORKOUTS = "TEN_WORKOUTS";
    public static final String TWENTY_FIVE_WORKOUTS = "TWENTY_FIVE_WORKOUTS";
    public static final String FIFTY_WORKOUTS = "FIFTY_WORKOUTS";
    public static final String HUNDRED_WORKOUTS = "HUNDRED_WORKOUTS";
    public static final String FIRST_ROUTINE_CREATED = "FIRST_ROUTINE_CREATED";
    public static final String XP_1000 = "XP_1000";
    public static final String XP_5000 = "XP_5000";
    public static final String XP_10000 = "XP_10000";

    public AchievementService(
            AchievementRepository achievementRepo,
            UserAchievementRepository userAchievementRepo,
            WorkoutSessionRepository workoutSessionRepo,
            RoutineTemplateRepository routineTemplateRepo,
            XpService xpService
    ) {
        this.achievementRepo = achievementRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.routineTemplateRepo = routineTemplateRepo;
        this.xpService = xpService;
    }

    /**
     * Obtiene todas las hazañas con el estado de desbloqueo para un usuario
     */
    @Transactional(readOnly = true)
    public List<AchievementDto> getAllAchievementsForUser(User user) {
        List<Achievement> allAchievements = achievementRepo.findByIsActiveTrueOrderBySortOrderAsc();
        Set<String> unlockedCodes = userAchievementRepo.findUnlockedAchievementCodesByUserId(user.getId());
        
        // Obtener fechas de desbloqueo
        Map<String, LocalDateTime> unlockDates = new HashMap<>();
        userAchievementRepo.findByUser_IdOrderByUnlockedAtDesc(user.getId())
                .forEach(ua -> unlockDates.put(ua.getAchievement().getCode(), ua.getUnlockedAt()));

        return allAchievements.stream()
                .map(a -> new AchievementDto(
                        a.getId(),
                        a.getCode(),
                        a.getName(),
                        a.getDescription(),
                        a.getIcon(),
                        a.getCategory(),
                        a.getXpReward(),
                        unlockedCodes.contains(a.getCode()),
                        unlockDates.get(a.getCode())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene hazañas recién desbloqueadas (no vistas)
     */
    @Transactional(readOnly = true)
    public List<UserAchievementDto> getUnseenAchievements(User user) {
        return userAchievementRepo.findByUser_IdAndSeenFalseOrderByUnlockedAtDesc(user.getId())
                .stream()
                .map(ua -> new UserAchievementDto(
                        ua.getAchievement().getCode(),
                        ua.getAchievement().getName(),
                        ua.getAchievement().getDescription(),
                        ua.getAchievement().getIcon(),
                        ua.getAchievement().getXpReward(),
                        ua.getUnlockedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Marca hazañas como vistas
     */
    @Transactional
    public void markAchievementsAsSeen(User user, List<String> achievementCodes) {
        for (String code : achievementCodes) {
            userAchievementRepo.findByUser_IdAndAchievement_Code(user.getId(), code)
                    .ifPresent(ua -> {
                        ua.setSeen(true);
                        userAchievementRepo.save(ua);
                    });
        }
    }

    /**
     * Verifica y otorga hazañas basadas en entrenamientos completados.
     * Llamar después de completar un workout.
     */
    @Transactional
    public List<UserAchievementDto> checkWorkoutAchievements(User user) {
        List<UserAchievementDto> newAchievements = new ArrayList<>();
        
        long completedWorkouts = workoutSessionRepo.countByUser_IdAndStatus(
                user.getId(), 
                WorkoutSessionStatus.COMPLETED
        );

        // Primera rutina
        if (completedWorkouts >= 1) {
            tryUnlockAchievement(user, FIRST_WORKOUT).ifPresent(newAchievements::add);
        }

        // 10 entrenamientos
        if (completedWorkouts >= 10) {
            tryUnlockAchievement(user, TEN_WORKOUTS).ifPresent(newAchievements::add);
        }

        // 25 entrenamientos
        if (completedWorkouts >= 25) {
            tryUnlockAchievement(user, TWENTY_FIVE_WORKOUTS).ifPresent(newAchievements::add);
        }

        // 50 entrenamientos
        if (completedWorkouts >= 50) {
            tryUnlockAchievement(user, FIFTY_WORKOUTS).ifPresent(newAchievements::add);
        }

        // 100 entrenamientos
        if (completedWorkouts >= 100) {
            tryUnlockAchievement(user, HUNDRED_WORKOUTS).ifPresent(newAchievements::add);
        }

        return newAchievements;
    }

    /**
     * Verifica y otorga hazañas basadas en rutinas creadas.
     * Llamar después de crear una rutina.
     */
    @Transactional
    public List<UserAchievementDto> checkRoutineCreationAchievements(User user) {
        List<UserAchievementDto> newAchievements = new ArrayList<>();
        
        long routinesCreated = routineTemplateRepo.countByUser_Id(user.getId());

        if (routinesCreated >= 1) {
            tryUnlockAchievement(user, FIRST_ROUTINE_CREATED).ifPresent(newAchievements::add);
        }

        return newAchievements;
    }

    /**
     * Verifica y otorga hazañas basadas en XP acumulado.
     * Llamar después de ganar XP.
     */
    @Transactional
    public List<UserAchievementDto> checkXpAchievements(User user) {
        List<UserAchievementDto> newAchievements = new ArrayList<>();
        
        int lifetimeXp = user.getLifetimeXp() != null ? user.getLifetimeXp() : 0;

        if (lifetimeXp >= 1000) {
            tryUnlockAchievement(user, XP_1000).ifPresent(newAchievements::add);
        }

        if (lifetimeXp >= 5000) {
            tryUnlockAchievement(user, XP_5000).ifPresent(newAchievements::add);
        }

        if (lifetimeXp >= 10000) {
            tryUnlockAchievement(user, XP_10000).ifPresent(newAchievements::add);
        }

        return newAchievements;
    }

    /**
     * Intenta desbloquear una hazaña para el usuario.
     * Retorna el DTO si se desbloqueó, vacío si ya la tenía.
     */
    @Transactional
    public Optional<UserAchievementDto> tryUnlockAchievement(User user, String achievementCode) {
        // Verificar si ya la tiene
        if (userAchievementRepo.existsByUser_IdAndAchievement_Code(user.getId(), achievementCode)) {
            return Optional.empty();
        }

        // Buscar la hazaña
        Optional<Achievement> achievementOpt = achievementRepo.findByCode(achievementCode);
        if (achievementOpt.isEmpty()) {
            return Optional.empty();
        }

        Achievement achievement = achievementOpt.get();

        // Crear el registro de desbloqueo
        UserAchievement userAchievement = new UserAchievement(user, achievement);
        userAchievementRepo.save(userAchievement);

        // Otorgar XP si tiene recompensa
        if (achievement.getXpReward() > 0) {
            xpService.grantXp(
                    user,
                    achievement.getXpReward(),
                    XpEventType.ACHIEVEMENT_UNLOCKED,
                    "Hazaña desbloqueada: " + achievement.getName()
            );
        }

        return Optional.of(new UserAchievementDto(
                achievement.getCode(),
                achievement.getName(),
                achievement.getDescription(),
                achievement.getIcon(),
                achievement.getXpReward(),
                userAchievement.getUnlockedAt()
        ));
    }

    /**
     * Obtiene estadísticas de hazañas para un usuario
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAchievementStats(User user) {
        long totalAchievements = achievementRepo.count();
        long unlockedAchievements = userAchievementRepo.countByUser_Id(user.getId());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalAchievements);
        stats.put("unlocked", unlockedAchievements);
        stats.put("progress", totalAchievements > 0 ? (int) ((unlockedAchievements * 100) / totalAchievements) : 0);
        
        return stats;
    }
}
