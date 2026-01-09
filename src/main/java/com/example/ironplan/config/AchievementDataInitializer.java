package com.example.ironplan.config;

import com.example.ironplan.model.Achievement;
import com.example.ironplan.repository.AchievementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inicializa las hazañas en la base de datos si no existen.
 */
@Component
public class AchievementDataInitializer implements CommandLineRunner {

    private final AchievementRepository achievementRepo;

    public AchievementDataInitializer(AchievementRepository achievementRepo) {
        this.achievementRepo = achievementRepo;
    }

    @Override
    public void run(String... args) {
        // Solo inicializar si no hay hazañas
        if (achievementRepo.count() > 0) {
            return;
        }

        List<Achievement> achievements = List.of(
                // Hazañas de entrenamiento
                new Achievement(
                        "FIRST_WORKOUT",
                        "Primera Rutina",
                        "Completa tu primer entrenamiento",
                        "trophy",
                        "workout",
                        50,
                        1
                ),
                new Achievement(
                        "TEN_WORKOUTS",
                        "10 Entrenamientos",
                        "Completa 10 entrenamientos",
                        "medal",
                        "workout",
                        100,
                        2
                ),
                new Achievement(
                        "TWENTY_FIVE_WORKOUTS",
                        "25 Entrenamientos",
                        "Completa 25 entrenamientos",
                        "medal",
                        "workout",
                        200,
                        3
                ),
                new Achievement(
                        "FIFTY_WORKOUTS",
                        "50 Entrenamientos",
                        "Completa 50 entrenamientos",
                        "star",
                        "workout",
                        500,
                        4
                ),
                new Achievement(
                        "HUNDRED_WORKOUTS",
                        "100 Entrenamientos",
                        "Completa 100 entrenamientos. ¡Eres imparable!",
                        "crown",
                        "workout",
                        1000,
                        5
                ),

                // Hazañas de creación
                new Achievement(
                        "FIRST_ROUTINE_CREATED",
                        "Creador",
                        "Crea tu primera rutina personalizada",
                        "pencil",
                        "creator",
                        75,
                        10
                ),

                // Hazañas de XP
                new Achievement(
                        "XP_1000",
                        "1,000 XP",
                        "Acumula 1,000 XP de por vida",
                        "bolt",
                        "xp",
                        0, // No da XP extra para evitar loop
                        20
                ),
                new Achievement(
                        "XP_5000",
                        "5,000 XP",
                        "Acumula 5,000 XP de por vida",
                        "bolt",
                        "xp",
                        0,
                        21
                ),
                new Achievement(
                        "XP_10000",
                        "10,000 XP",
                        "Acumula 10,000 XP de por vida. ¡Leyenda!",
                        "fire",
                        "xp",
                        0,
                        22
                )
        );

        achievementRepo.saveAll(achievements);
        System.out.println("✅ Hazañas inicializadas: " + achievements.size());
    }
}
