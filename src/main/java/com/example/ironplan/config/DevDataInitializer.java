package com.example.ironplan.config;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Puebla MySQL local con datos de demo. Solo activo con perfil {@code dev}.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final PasswordEncoder passwordEncoder;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final OrganizationalGroupRepository groupRepository;
    private final OrganizationalGroupMemberRepository memberRepository;
    private final OrganizationalInvitationRepository invitationRepository;
    private final RoutineTemplateRepository routineRepository;
    private final UserUnlockedRoutineRepository unlockedRoutineRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final UserXpEventRepository xpEventRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserActivityRepository activityRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final CompetitionMemberParticipantRepository competitionMemberParticipantRepository;
    private final NotificationRepository notificationRepository;
    private final AchievementRepository achievementRepository;
    private final DevMetricsEnhancer metricsEnhancer;

    private Map<String, Exercise> exercisesByName = new LinkedHashMap<>();
    private User admin;
    private User marina;
    private User carlos;
    private User lucia;
    private OrganizationalGroup empresa;
    private OrganizationalGroup facultad;
    private OrganizationalGroup carrera;
    private OrganizationalGroup grupoManana;
    private RoutineTemplate hipertrofia;
    private RoutineTemplate fuerza;
    private RoutineTemplate premium;
    private RoutineTemplate marinaRoutine;

    public DevDataInitializer(
            PasswordEncoder passwordEncoder,
            ExerciseRepository exerciseRepository,
            UserRepository userRepository,
            OrganizationalGroupRepository groupRepository,
            OrganizationalGroupMemberRepository memberRepository,
            OrganizationalInvitationRepository invitationRepository,
            RoutineTemplateRepository routineRepository,
            UserUnlockedRoutineRepository unlockedRoutineRepository,
            WorkoutSessionRepository workoutSessionRepository,
            WorkoutExerciseRepository workoutExerciseRepository,
            WorkoutSetRepository workoutSetRepository,
            UserXpEventRepository xpEventRepository,
            UserAchievementRepository userAchievementRepository,
            UserActivityRepository activityRepository,
            CompetitionRepository competitionRepository,
            CompetitionParticipantRepository competitionParticipantRepository,
            CompetitionMemberParticipantRepository competitionMemberParticipantRepository,
            NotificationRepository notificationRepository,
            AchievementRepository achievementRepository,
            DevMetricsEnhancer metricsEnhancer) {
        this.passwordEncoder = passwordEncoder;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.routineRepository = routineRepository;
        this.unlockedRoutineRepository = unlockedRoutineRepository;
        this.workoutSessionRepository = workoutSessionRepository;
        this.workoutExerciseRepository = workoutExerciseRepository;
        this.workoutSetRepository = workoutSetRepository;
        this.xpEventRepository = xpEventRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.activityRepository = activityRepository;
        this.competitionRepository = competitionRepository;
        this.competitionParticipantRepository = competitionParticipantRepository;
        this.competitionMemberParticipantRepository = competitionMemberParticipantRepository;
        this.notificationRepository = notificationRepository;
        this.achievementRepository = achievementRepository;
        this.metricsEnhancer = metricsEnhancer;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(DevSeedData.EMAIL_MARINA)) {
            log.info("Dev seed ya aplicado ({} existe). Omitiendo.", DevSeedData.EMAIL_MARINA);
            return;
        }

        log.info("Iniciando dev seed de IronPlan...");
        seedExercises();
        seedUsers();
        seedOrganization();
        seedRoutines();
        linkUserRoutinesAndProgress();
        seedWorkouts();
        seedXpAchievementsAndActivities();
        seedCompetitions();
        seedNotifications();
        metricsEnhancer.enrichAfterInitialSeed();
        log.info("Dev seed completado. Login demo: {} / {}", DevSeedData.EMAIL_MARINA, DevSeedData.DEMO_PASSWORD);
    }

    private void seedExercises() {
        for (String[] row : DevSeedData.EXERCISES) {
            Exercise ex = new Exercise();
            ex.setName(row[0]);
            ex.setDescription(row[1]);
            ex.setInstructions(row[2]);
            ex.setPrimaryMuscle(row[3]);
            ex.setSecondaryMuscle(row[4]);
            exerciseRepository.save(ex);
            exercisesByName.put(row[0], ex);
        }
        log.info("Ejercicios: {}", exercisesByName.size());
    }

    private void seedUsers() {
        String hash = passwordEncoder.encode(DevSeedData.DEMO_PASSWORD);
        LocalDate birthday = LocalDate.of(1998, 6, 15);

        admin = saveUser("admin", DevSeedData.EMAIL_ADMIN, hash, Role.ADMIN, "Admin", "IronPlan",
                birthday, Level.AVANZADO, 5, Goal.HIPERTROFIA, Gender.MASCULINO, 82, 178, 500, 500);

        marina = saveUser("marina", DevSeedData.EMAIL_MARINA, hash, Role.USER, "Marina", "Parodi",
                birthday, Level.INTERMEDIO, 4, Goal.HIPERTROFIA, Gender.FEMENINO, 62, 165, 0, 0);

        carlos = saveUser("carlos", DevSeedData.EMAIL_CARLOS, hash, Role.USER, "Carlos", "Méndez",
                LocalDate.of(1997, 3, 10), Level.INTERMEDIO, 3, Goal.FUERZA, Gender.MASCULINO, 78, 175, 800, 800);

        lucia = saveUser("lucia", DevSeedData.EMAIL_LUCIA, hash, Role.USER, "Lucía", "Fernández",
                LocalDate.of(2000, 11, 22), Level.NOVATO, 3, Goal.RESISTENCIA, Gender.FEMENINO, 58, 162, 200, 200);

        log.info("Usuarios demo: 4");
    }

    private User saveUser(String username, String email, String hash, Role role,
                          String firstName, String lastName, LocalDate birthday,
                          Level level, int trainDays, Goal goal, Gender gender,
                          int weight, int height, int xpPoints, int lifetimeXp) {
        User u = User.builder()
                .username(username)
                .email(email)
                .password(hash)
                .firstName(firstName)
                .lastName(lastName)
                .birthday(birthday)
                .level(level)
                .trainDays(trainDays)
                .goal(goal)
                .gender(gender)
                .weight(weight)
                .height(height)
                .role(role)
                .xpPoints(xpPoints)
                .lifetimeXp(lifetimeXp)
                .xpRank(XpRank.fromLifetimeXp(lifetimeXp))
                .acceptedTerms(true)
                .acceptedPrivacy(true)
                .consentProgramMetrics(true)
                .build();
        return userRepository.save(u);
    }

    private void seedOrganization() {
        empresa = saveGroup("IronPlan Corp", GroupType.EMPRESA, "IRONPLAN", null,
                OrganizationKind.EMPRESA, admin);
        facultad = saveGroup("Facultad de Salud", GroupType.FACULTAD, "FAC-SALUD", empresa, null, admin);
        carrera = saveGroup("Kinesiología", GroupType.CARRERA, "CARR-KINE", facultad, null, admin);
        grupoManana = saveGroup("Grupo Mañana", GroupType.GRUPO, "GRP-MANANA", carrera, null, admin);

        marina.setPrimaryOrganizationalGroup(carrera);
        marina.setOrganizationCode(DevSeedData.INVITE_CODE);
        marina.setOrganizationGroup(carrera.getName());
        marina.setOrganizationRole("MEMBER");
        carlos.setPrimaryOrganizationalGroup(carrera);
        carlos.setOrganizationCode(DevSeedData.INVITE_CODE);
        carlos.setOrganizationGroup(carrera.getName());
        carlos.setOrganizationRole("ADMIN");
        userRepository.save(marina);
        userRepository.save(carlos);

        saveMember(marina, carrera, GroupMembershipRole.MEMBER);
        saveMember(carlos, carrera, GroupMembershipRole.ADMIN);
        saveMember(marina, grupoManana, GroupMembershipRole.MEMBER);
        saveMember(carlos, grupoManana, GroupMembershipRole.MEMBER);

        OrganizationalInvitation inv = OrganizationalInvitation.builder()
                .code(DevSeedData.INVITE_CODE)
                .group(carrera)
                .maxUses(50)
                .usesCount(2)
                .createdBy(admin)
                .membershipRole(GroupMembershipRole.MEMBER)
                .active(true)
                .expiresAt(LocalDate.of(2026, 12, 31))
                .build();
        invitationRepository.save(inv);
        log.info("Organización demo creada");
    }

    private OrganizationalGroup saveGroup(String name, GroupType type, String code,
                                          OrganizationalGroup parent, OrganizationKind kind, User creator) {
        OrganizationalGroup g = OrganizationalGroup.builder()
                .name(name)
                .groupType(type)
                .code(code)
                .parent(parent)
                .organizationKind(kind)
                .createdBy(creator)
                .active(true)
                .build();
        return groupRepository.save(g);
    }

    private void saveMember(User user, OrganizationalGroup group, GroupMembershipRole role) {
        memberRepository.save(OrganizationalGroupMember.builder()
                .user(user)
                .group(group)
                .role(role)
                .active(true)
                .build());
    }

    private void seedRoutines() {
        hipertrofia = buildHipertrofiaRoutine();
        fuerza = buildFuerzaRoutine();
        premium = buildPremiumRoutine();
        marinaRoutine = buildMarinaRoutine();
        routineRepository.saveAll(List.of(hipertrofia, fuerza, premium, marinaRoutine));
        log.info("Rutinas: 4");
    }

    private RoutineTemplate buildHipertrofiaRoutine() {
        RoutineTemplate r = baseRoutine("Hipertrofia 4 días", admin, Goal.HIPERTROFIA,
                Access_Type.FREE, 0, 4, Type.ADMIN_ISOLATED, true, 8);
        r.setDescription("Rutina de hipertrofia para 4 días por semana.");
        r.setLongDescription("Dos mesociclos de 4 semanas con progresión en volumen.");

        RoutineBlock block1 = block(r, "Mesociclo 1", 1, 4);
        addSession(block1, 1, "Empuje", "Pecho, Hombros, Tríceps",
                sessionExercises("Press de banca", 4, 8, 10, 2, 120),
                sessionExercises("Press inclinado con mancuernas", 3, 10, 12, 2, 90),
                sessionExercises("Press militar", 3, 8, 10, 2, 120),
                sessionExercises("Elevaciones laterales", 3, 12, 15, 1, 60),
                sessionExercises("Extensiones de tríceps en polea", 3, 10, 12, 1, 60));
        addSession(block1, 2, "Tirón", "Espalda, Bíceps",
                sessionExercises("Remo con barra", 4, 8, 10, 2, 120),
                sessionExercises("Jalón al pecho", 3, 10, 12, 2, 90),
                sessionExercises("Face pull", 3, 15, 20, 1, 60),
                sessionExercises("Curl de bíceps con barra", 3, 10, 12, 1, 60),
                sessionExercises("Curl martillo", 3, 10, 12, 1, 60));
        addSession(block1, 3, "Piernas", "Piernas, Glúteos",
                sessionExercises("Sentadilla trasera", 4, 6, 8, 2, 180),
                sessionExercises("Peso muerto rumano", 3, 8, 10, 2, 120),
                sessionExercises("Prensa de piernas", 3, 12, 15, 1, 90),
                sessionExercises("Zancadas con mancuernas", 3, 10, 12, 2, 90),
                sessionExercises("Elevación de gemelos de pie", 4, 12, 15, 1, 60));
        addSession(block1, 4, "Brazos + hombros", "Brazos, Hombros",
                sessionExercises("Press de hombros con mancuernas", 3, 8, 10, 2, 90),
                sessionExercises("Elevaciones laterales", 4, 12, 15, 1, 60),
                sessionExercises("Curl de bíceps con barra", 3, 10, 12, 1, 60),
                sessionExercises("Extensiones de tríceps en polea", 3, 10, 12, 1, 60),
                sessionExercises("Plancha abdominal", 3, 30, 45, 0, 60));

        RoutineBlock block2 = block(r, "Mesociclo 2", 2, 4);
        addSession(block2, 1, "Empuje B", "Pecho, Tríceps",
                sessionExercises("Press de banca", 4, 6, 8, 2, 150),
                sessionExercises("Aperturas en polea", 3, 12, 15, 1, 60),
                sessionExercises("Press militar", 3, 6, 8, 2, 120),
                sessionExercises("Fondos en paralelas", 3, 8, 12, 2, 90),
                sessionExercises("Extensiones de tríceps en polea", 3, 8, 10, 1, 60));
        addSession(block2, 2, "Tirón B", "Espalda",
                sessionExercises("Peso muerto convencional", 3, 5, 6, 2, 180),
                sessionExercises("Remo en máquina", 4, 8, 10, 2, 90),
                sessionExercises("Dominadas", 3, 6, 10, 2, 120),
                sessionExercises("Pullover en polea", 3, 12, 15, 1, 60),
                sessionExercises("Curl martillo", 3, 10, 12, 1, 60));
        addSession(block2, 3, "Piernas B", "Piernas",
                sessionExercises("Sentadilla trasera", 4, 5, 7, 2, 180),
                sessionExercises("Hip thrust con barra", 4, 8, 10, 2, 120),
                sessionExercises("Prensa de piernas", 3, 10, 12, 1, 90),
                sessionExercises("Peso muerto rumano", 3, 8, 10, 2, 120),
                sessionExercises("Elevación de gemelos de pie", 4, 10, 12, 1, 60));
        addSession(block2, 4, "Full upper", "Torso",
                sessionExercises("Press de banca", 3, 8, 10, 2, 120),
                sessionExercises("Remo con barra", 3, 8, 10, 2, 120),
                sessionExercises("Press militar", 3, 8, 10, 2, 90),
                sessionExercises("Curl de bíceps con barra", 3, 10, 12, 1, 60),
                sessionExercises("Crunch en polea", 3, 12, 15, 1, 60));

        r.addBlock(block1);
        r.addBlock(block2);
        return r;
    }

    private RoutineTemplate buildFuerzaRoutine() {
        RoutineTemplate r = baseRoutine("Fuerza 3 días", admin, Goal.FUERZA,
                Access_Type.FREE, 0, 3, Type.ADMIN_ISOLATED, true, 6);
        r.setDescription("Rutina de fuerza para 3 días.");
        RoutineBlock block = block(r, "Bloque principal", 1, 6);
        addSession(block, 1, "Día A — Sentadilla", "Piernas",
                sessionExercises("Sentadilla trasera", 5, 3, 5, 3, 180),
                sessionExercises("Prensa de piernas", 3, 8, 10, 2, 120),
                sessionExercises("Elevación de gemelos de pie", 4, 8, 10, 2, 90));
        addSession(block, 2, "Día B — Press", "Pecho, Hombros",
                sessionExercises("Press de banca", 5, 3, 5, 3, 180),
                sessionExercises("Press militar", 4, 4, 6, 2, 120),
                sessionExercises("Fondos en paralelas", 3, 6, 8, 2, 90));
        addSession(block, 3, "Día C — Peso muerto", "Espalda, Piernas",
                sessionExercises("Peso muerto convencional", 5, 3, 5, 3, 180),
                sessionExercises("Remo con barra", 4, 6, 8, 2, 120),
                sessionExercises("Dominadas", 3, 5, 8, 2, 120));
        r.addBlock(block);
        return r;
    }

    private RoutineTemplate buildPremiumRoutine() {
        RoutineTemplate r = baseRoutine("Push/Pull premium", admin, Goal.HIPERTROFIA,
                Access_Type.XP_UNLOCK, 500, 4, Type.ADMIN_ISOLATED, true, 4);
        r.setDescription("Rutina premium desbloqueable con XP.");
        RoutineBlock block = block(r, "Fase única", 1, 4);
        addSession(block, 1, "Push", "Pecho, Hombros, Tríceps",
                sessionExercises("Press de banca", 4, 8, 10, 2, 120),
                sessionExercises("Press inclinado con mancuernas", 3, 10, 12, 2, 90),
                sessionExercises("Elevaciones laterales", 4, 12, 15, 1, 60),
                sessionExercises("Extensiones de tríceps en polea", 3, 10, 12, 1, 60));
        addSession(block, 2, "Pull", "Espalda, Bíceps",
                sessionExercises("Remo con barra", 4, 8, 10, 2, 120),
                sessionExercises("Jalón al pecho", 3, 10, 12, 2, 90),
                sessionExercises("Face pull", 3, 15, 20, 1, 60),
                sessionExercises("Curl de bíceps con barra", 3, 10, 12, 1, 60));
        r.addBlock(block);
        return r;
    }

    private RoutineTemplate buildMarinaRoutine() {
        RoutineTemplate r = baseRoutine("Full body Marina", marina, Goal.HIPERTROFIA,
                Access_Type.USER_SHARED, 0, 3, Type.USER_CREATED, true, 4);
        r.setDescription("Rutina compartida por Marina.");
        RoutineBlock block = block(r, "Semanas 1-4", 1, 4);
        addSession(block, 1, "Full A", "Cuerpo completo",
                sessionExercises("Sentadilla trasera", 3, 8, 10, 2, 120),
                sessionExercises("Press de banca", 3, 8, 10, 2, 120),
                sessionExercises("Remo con barra", 3, 8, 10, 2, 90),
                sessionExercises("Plancha abdominal", 3, 30, 45, 0, 60));
        addSession(block, 2, "Full B", "Cuerpo completo",
                sessionExercises("Peso muerto rumano", 3, 8, 10, 2, 120),
                sessionExercises("Press militar", 3, 8, 10, 2, 90),
                sessionExercises("Dominadas", 3, 6, 10, 2, 120),
                sessionExercises("Crunch en polea", 3, 12, 15, 1, 60));
        r.addBlock(block);
        return r;
    }

    private RoutineTemplate baseRoutine(String name, User owner, Goal goal, Access_Type access,
                                        int xpCost, int daysPerWeek, Type type, boolean isPublic, int durationWeeks) {
        RoutineTemplate r = new RoutineTemplate();
        r.setName(name);
        r.setDescription(name);
        r.setLongDescription("Rutina de demostración IronPlan.");
        r.setGoal(goal);
        r.setUser(owner);
        r.setIsPublic(isPublic);
        r.setType(type);
        r.setAccess(access);
        r.setXp_cost(xpCost);
        r.setDays_per_week(daysPerWeek);
        r.setXp_gain(75);
        r.setStatus(RoutineStatus.PUBLISHED);
        r.setCreatedAt(LocalDateTime.now());
        r.setSuggestedLevel(Level.INTERMEDIO);
        r.setRoutineGender(RoutineGender.UNISEX);
        r.setDurationWeeks(durationWeeks);
        r.setUsageCount(12);
        return r;
    }

    private RoutineBlock block(RoutineTemplate routine, String name, int order, int weeks) {
        RoutineBlock b = new RoutineBlock();
        b.setName(name);
        b.setOrderIndex(order);
        b.setDurationWeeks(weeks);
        b.setDescription("Bloque " + name);
        return b;
    }

    private void addSession(RoutineBlock block, int order, String title, String muscles,
                            ExercisePlan... plans) {
        RoutineDetail session = new RoutineDetail();
        session.setSessionOrder(order);
        session.setTitle(title);
        session.setMuscles(muscles);
        session.setEstimatedMinutes(55);
        session.setEstimatedXp(75);
        session.setTotalSeries(plans.length * 3);
        int exOrder = 1;
        for (ExercisePlan plan : plans) {
            RoutineExercise re = new RoutineExercise();
            re.setExercise(exercisesByName.get(plan.name));
            re.setDisplayName(plan.name);
            re.setExerciseOrder(exOrder++);
            re.setSets(plan.sets);
            re.setRepsMin(plan.repsMin);
            re.setRepsMax(plan.repsMax);
            re.setRir(plan.rir);
            re.setRestMinutes(plan.restSec / 60);
            session.addExercise(re);
        }
        block.addSession(session);
    }

    private ExercisePlan sessionExercises(String name, int sets, int repsMin, int repsMax, int rir, int restSec) {
        return new ExercisePlan(name, sets, repsMin, repsMax, rir, restSec);
    }

    private record ExercisePlan(String name, int sets, int repsMin, int repsMax, int rir, int restSec) {}

    private void linkUserRoutinesAndProgress() {
        marina.setCurrentRoutine(hipertrofia);
        marina.setRoutineStartedAt(LocalDateTime.now().minusWeeks(2));
        marina.setLifetimeXp(1250);
        marina.setXpPoints(1250);
        marina.setXpRank(XpRank.fromLifetimeXp(1250));
        userRepository.save(marina);

        unlock(marina, hipertrofia);
        unlock(marina, fuerza);
        unlock(marina, premium);
        unlock(carlos, fuerza);
    }

    private void unlock(User user, RoutineTemplate routine) {
        UserUnlockedRoutine uur = new UserUnlockedRoutine();
        uur.setUser(user);
        uur.setRoutine(routine);
        uur.setUnlockedAt(LocalDateTime.now().minusDays(10));
        unlockedRoutineRepository.save(uur);
    }

    private void seedWorkouts() {
        List<RoutineDetail> sessions = hipertrofia.getBlocks().get(0).getSessions();
        createCompletedWorkout(marina, sessions.get(0), 12, 80);
        createCompletedWorkout(marina, sessions.get(1), 9, 85);
        createCompletedWorkout(marina, sessions.get(2), 5, 90);
        createCompletedWorkout(marina, sessions.get(3), 2, 75);
        createCompletedWorkout(carlos, fuerza.getBlocks().get(0).getSessions().get(0), 7, 70);
        log.info("Workouts completados: 5");
    }

    private void createCompletedWorkout(User user, RoutineDetail session, int daysAgo, int xpEarned) {
        LocalDateTime started = LocalDateTime.now().minusDays(daysAgo).withHour(10).withMinute(0);
        LocalDateTime completed = started.plusMinutes(52);

        WorkoutSession ws = new WorkoutSession();
        ws.setUser(user);
        ws.setRoutineDetail(session);
        ws.setStatus(WorkoutSessionStatus.COMPLETED);
        ws.setStartedAt(started);
        ws.setCompletedAt(completed);
        int totalEx = session.getExercises().size();
        ws.setTotalExercises(totalEx);
        ws.setCompletedExercises(totalEx);
        ws.setProgressPercentage(100.0);
        ws.setXpEarned(xpEarned);
        workoutSessionRepository.save(ws);

        int order = 1;
        for (RoutineExercise re : session.getExercises()) {
            WorkoutExercise we = new WorkoutExercise();
            we.setWorkoutSession(ws);
            we.setRoutineExercise(re);
            we.setExercise(re.getExercise());
            we.setExerciseName(re.getDisplayName());
            we.setExerciseOrder(order++);
            we.setPlannedSets(re.getSets());
            we.setPlannedRepsMin(re.getRepsMin());
            we.setPlannedRepsMax(re.getRepsMax());
            we.setPlannedRir(re.getRir());
            we.setPlannedRestSeconds(re.getRestMinutes() != null ? re.getRestMinutes() * 60 : 90);
            we.setStatus(WorkoutExerciseStatus.COMPLETED);
            we.setCompletedSets(re.getSets());
            we.setStartedAt(started);
            we.setFinishedAt(completed);
            workoutExerciseRepository.save(we);

            int reps = (re.getRepsMin() + re.getRepsMax()) / 2;
            double baseWeight = weightForExercise(re.getExercise().getPrimaryMuscle());
            for (int s = 1; s <= re.getSets(); s++) {
                WorkoutSet set = new WorkoutSet();
                set.setWorkoutExercise(we);
                set.setSetNumber(s);
                set.setReps(reps);
                set.setWeightKg(baseWeight + (s - 1) * 2.5);
                set.setCompleted(true);
                if (s > 1) {
                    set.setPreviousReps(reps);
                    set.setPreviousWeightKg(baseWeight);
                }
                workoutSetRepository.save(set);
            }
        }
    }

    private double weightForExercise(String muscle) {
        return switch (muscle) {
            case "Piernas" -> 60.0;
            case "Espalda" -> 50.0;
            case "Pecho" -> 45.0;
            case "Hombros" -> 25.0;
            case "Brazos" -> 15.0;
            case "Core" -> 0.0;
            default -> 30.0;
        };
    }

    private void seedXpAchievementsAndActivities() {
        UserXpEvent e1 = xpEvent(marina, 80, XpEventType.WORKOUT_COMPLETED, "Sesión Empuje", hipertrofia, 12);
        UserXpEvent e2 = xpEvent(marina, 85, XpEventType.WORKOUT_COMPLETED, "Sesión Tirón", hipertrofia, 9);
        UserXpEvent e3 = xpEvent(marina, 90, XpEventType.WORKOUT_COMPLETED, "Sesión Piernas", hipertrofia, 5);
        UserXpEvent e4 = xpEvent(marina, 75, XpEventType.WORKOUT_COMPLETED, "Sesión Brazos", hipertrofia, 2);
        UserXpEvent unlockPremium = xpEvent(marina, -500, XpEventType.ROUTINE_PURCHASE, "Desbloqueo Push/Pull premium", premium, 8);
        xpEventRepository.saveAll(List.of(e1, e2, e3, e4, unlockPremium));

        achievementRepository.findByCode("FIRST_WORKOUT").ifPresent(a -> {
            UserAchievement ua = new UserAchievement(marina, a);
            ua.setSeen(true);
            ua.setUnlockedAt(LocalDateTime.now().minusDays(12));
            userAchievementRepository.save(ua);
        });
        achievementRepository.findByCode("TEN_WORKOUTS").ifPresent(a -> {
            UserAchievement ua = new UserAchievement(marina, a);
            ua.setSeen(false);
            ua.setUnlockedAt(LocalDateTime.now().minusDays(1));
            userAchievementRepository.save(ua);
        });

        LocalDate today = LocalDate.now();
        for (int d = 0; d < 14; d++) {
            LocalDate date = today.minusDays(d);
            if (d % 3 == 0) {
                saveActivity(marina, date, MetricType.WORKOUTS_COUNT, 1.0, null);
                saveActivity(marina, date, MetricType.ACTIVE_MINUTES, 52.0, null);
            }
            if (d % 4 == 0) {
                saveActivity(carlos, date, MetricType.WORKOUTS_COUNT, 1.0, null);
                saveActivity(carlos, date, MetricType.ACTIVE_MINUTES, 48.0, null);
            }
        }
    }

    private UserXpEvent xpEvent(User user, int delta, XpEventType type, String desc,
                                RoutineTemplate routine, int daysAgo) {
        UserXpEvent e = new UserXpEvent();
        e.setUser(user);
        e.setXpDelta(delta);
        e.setType(type);
        e.setDescription(desc);
        e.setRoutineTemplate(routine);
        e.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return e;
    }

    private void saveActivity(User user, LocalDate date, MetricType metric, double value, Long sourceId) {
        activityRepository.save(UserActivity.builder()
                .user(user)
                .activityDate(date)
                .metricType(metric)
                .metricValue(value)
                .sourceId(sourceId)
                .build());
    }

    private void seedCompetitions() {
        Competition active = Competition.builder()
                .name("Reto Marzo 2026")
                .competitionType(CompetitionType.CHALLENGE)
                .scopeLevel(ScopeLevel.CARRERA)
                .scopeReference(carrera)
                .metricType(MetricType.WORKOUTS_COUNT)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(CompetitionStatus.ACTIVE)
                .createdBy(admin)
                .build();
        competitionRepository.save(active);

        competitionParticipantRepository.save(CompetitionParticipant.builder()
                .competition(active)
                .group(grupoManana)
                .groupScore(8.0)
                .rank(1)
                .lastCalculatedAt(LocalDateTime.now())
                .build());
        competitionParticipantRepository.save(CompetitionParticipant.builder()
                .competition(active)
                .group(carrera)
                .groupScore(5.0)
                .rank(2)
                .lastCalculatedAt(LocalDateTime.now())
                .build());

        competitionMemberParticipantRepository.save(CompetitionMemberParticipant.builder()
                .competition(active)
                .user(marina)
                .score(4.0)
                .rank(1)
                .lastCalculatedAt(LocalDateTime.now())
                .build());
        competitionMemberParticipantRepository.save(CompetitionMemberParticipant.builder()
                .competition(active)
                .user(carlos)
                .score(1.0)
                .rank(2)
                .lastCalculatedAt(LocalDateTime.now())
                .build());

        Competition finished = Competition.builder()
                .name("Ranking Q1")
                .competitionType(CompetitionType.RANKING)
                .scopeLevel(ScopeLevel.CARRERA)
                .scopeReference(carrera)
                .metricType(MetricType.ACTIVE_MINUTES)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(CompetitionStatus.FINISHED)
                .createdBy(admin)
                .build();
        competitionRepository.save(finished);

        competitionMemberParticipantRepository.save(CompetitionMemberParticipant.builder()
                .competition(finished)
                .user(carlos)
                .score(420.0)
                .rank(1)
                .lastCalculatedAt(LocalDateTime.now().minusDays(5))
                .build());
        competitionMemberParticipantRepository.save(CompetitionMemberParticipant.builder()
                .competition(finished)
                .user(marina)
                .score(380.0)
                .rank(2)
                .lastCalculatedAt(LocalDateTime.now().minusDays(5))
                .build());
        log.info("Competencias: 2");
    }

    private void seedNotifications() {
        notificationRepository.saveAll(List.of(
                new Notification(marina, NotificationType.SUCCESS, NotificationPriority.HIGH,
                        "¡Primera hazaña!", "Desbloqueaste Primera Rutina.", "/perfil/hazanas"),
                new Notification(marina, NotificationType.INFO, NotificationPriority.MEDIUM,
                        "Nuevo reto activo", "Reto Marzo 2026 ya está en marcha.",
                        "/competitions/1"),
                notifRead(marina, NotificationType.INFO, NotificationPriority.LOW,
                        "Sesión completada", "Empuje registrado con 80 XP.", "/perfil"),
                new Notification(marina, NotificationType.WARNING, NotificationPriority.MEDIUM,
                        "Hazaña sin ver", "Tienes logros nuevos por revisar.", "/perfil/hazanas"),
                notifRead(marina, NotificationType.SUCCESS, NotificationPriority.LOW,
                        "Rutina activa", "Hipertrofia 4 días es tu rutina actual.", "/mis-rutinas"),
                new Notification(marina, NotificationType.INFO, NotificationPriority.LOW,
                        "Invitación disponible", "Comparte KINE2026 con tus compañeros.", "/grupos/mis-grupos")
        ));
    }

    private Notification notifRead(User user, NotificationType type, NotificationPriority priority,
                                   String title, String message, String route) {
        Notification n = new Notification(user, type, priority, title, message, route);
        n.setRead(true);
        n.setReadAt(LocalDateTime.now().minusDays(1));
        return n;
    }
}
