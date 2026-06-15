package com.example.ironplan.config;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enriquece {@link UserActivity} y competencias para que leaderboards y métricas de grupos
 * se vean pobladas. Corre en perfil {@code dev} aunque el seed base ya exista.
 */
@Component
@Profile("dev")
@Order(100)
public class DevMetricsEnhancer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevMetricsEnhancer.class);
    private static final String MARKER_COMPETITION = "Reto IronPlan Demo";
    private static final long MIN_ACTIVITIES = 80;

    private final UserRepository userRepository;
    private final OrganizationalGroupRepository groupRepository;
    private final OrganizationalGroupMemberRepository memberRepository;
    private final UserActivityRepository activityRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final CompetitionMemberParticipantRepository competitionMemberParticipantRepository;
    private final PasswordEncoder passwordEncoder;

    public DevMetricsEnhancer(
            UserRepository userRepository,
            OrganizationalGroupRepository groupRepository,
            OrganizationalGroupMemberRepository memberRepository,
            UserActivityRepository activityRepository,
            WorkoutSessionRepository workoutSessionRepository,
            CompetitionRepository competitionRepository,
            CompetitionParticipantRepository competitionParticipantRepository,
            CompetitionMemberParticipantRepository competitionMemberParticipantRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.activityRepository = activityRepository;
        this.workoutSessionRepository = workoutSessionRepository;
        this.competitionRepository = competitionRepository;
        this.competitionParticipantRepository = competitionParticipantRepository;
        this.competitionMemberParticipantRepository = competitionMemberParticipantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(DevSeedData.EMAIL_MARINA)) {
            return;
        }
        if (competitionRepository.findAll().stream().anyMatch(c -> MARKER_COMPETITION.equals(c.getName()))) {
            return;
        }
        if (activityRepository.count() >= MIN_ACTIVITIES) {
            log.debug("Métricas demo ya enriquecidas ({} actividades).", activityRepository.count());
            return;
        }

        log.info("Enriqueciendo métricas y leaderboards de demo...");
        enrich();
        log.info("Métricas demo listas ({} actividades).", activityRepository.count());
    }

    /** Llamado al final del seed inicial para forzar métricas completas. */
    @Transactional
    public void enrichAfterInitialSeed() {
        if (competitionRepository.findAll().stream().anyMatch(c -> MARKER_COMPETITION.equals(c.getName()))) {
            return;
        }
        enrich();
    }

    private void enrich() {
        User admin = userRepository.findByEmail(DevSeedData.EMAIL_ADMIN).orElseThrow();
        User marina = userRepository.findByEmail(DevSeedData.EMAIL_MARINA).orElseThrow();
        User carlos = userRepository.findByEmail(DevSeedData.EMAIL_CARLOS).orElseThrow();

        OrganizationalGroup carrera = groupRepository.findByCode("CARR-KINE").orElseThrow();
        OrganizationalGroup grupoManana = groupRepository.findByCode("GRP-MANANA").orElseThrow();
        OrganizationalGroup grupoTarde = ensureGrupoTarde(carrera, admin);

        User pedro = ensureExtraUser("pedro", "pedro@ironplan.local", "Pedro", "Soto",
                carrera, grupoManana, Goal.HIPERTROFIA, 5);
        User sofia = ensureExtraUser("sofia", "sofia@ironplan.local", "Sofía", "López",
                carrera, grupoTarde, Goal.FUERZA, 4);

        syncWorkoutSessionsToActivities();
        seedRichActivities(marina, 12, 55);
        seedRichActivities(carlos, 9, 50);
        seedRichActivities(pedro, 7, 48);
        seedRichActivities(sofia, 6, 45);
        seedRichActivities(admin, 3, 40);

        ensureDemoCompetitions(admin, carrera, grupoManana, grupoTarde, marina, carlos, pedro, sofia);
    }

    private OrganizationalGroup ensureGrupoTarde(OrganizationalGroup carrera, User admin) {
        return groupRepository.findByCode("GRP-TARDE").orElseGet(() -> {
            OrganizationalGroup g = OrganizationalGroup.builder()
                    .name("Grupo Tarde")
                    .groupType(GroupType.GRUPO)
                    .code("GRP-TARDE")
                    .parent(carrera)
                    .active(true)
                    .createdBy(admin)
                    .build();
            return groupRepository.save(g);
        });
    }

    private User ensureExtraUser(String username, String email, String first, String last,
                                 OrganizationalGroup carrera, OrganizationalGroup primary,
                                 Goal goal, int workoutsBias) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        User u = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(DevSeedData.DEMO_PASSWORD))
                .firstName(first)
                .lastName(last)
                .birthday(LocalDate.of(1999, 1, 20))
                .level(Level.INTERMEDIO)
                .trainDays(4)
                .goal(goal)
                .gender(Gender.FEMENINO)
                .weight(65)
                .height(168)
                .role(Role.USER)
                .primaryOrganizationalGroup(primary)
                .organizationCode(DevSeedData.INVITE_CODE)
                .organizationGroup(carrera.getName())
                .organizationRole("MEMBER")
                .acceptedTerms(true)
                .acceptedPrivacy(true)
                .consentProgramMetrics(true)
                .build();
        u = userRepository.save(u);
        saveMember(u, carrera, GroupMembershipRole.MEMBER);
        saveMember(u, primary, GroupMembershipRole.MEMBER);
        return u;
    }

    private void saveMember(User user, OrganizationalGroup group, GroupMembershipRole role) {
        if (memberRepository.findByUserIdAndGroupId(user.getId(), group.getId()).isEmpty()) {
            memberRepository.save(OrganizationalGroupMember.builder()
                    .user(user)
                    .group(group)
                    .role(role)
                    .active(true)
                    .build());
        }
    }

    private void syncWorkoutSessionsToActivities() {
        List<WorkoutSession> completed = workoutSessionRepository.findAll().stream()
                .filter(ws -> ws.getStatus() == WorkoutSessionStatus.COMPLETED)
                .toList();

        for (WorkoutSession ws : completed) {
            LocalDate day = ws.getCompletedAt() != null
                    ? ws.getCompletedAt().toLocalDate()
                    : LocalDate.now();
            long minutes = 45;
            if (ws.getStartedAt() != null && ws.getCompletedAt() != null) {
                minutes = Math.max(30, java.time.Duration.between(ws.getStartedAt(), ws.getCompletedAt()).toMinutes());
            }

            saveActivityIfAbsent(ws.getUser(), day, MetricType.SESSIONS, 1.0, ws.getId());
            saveActivityIfAbsent(ws.getUser(), day, MetricType.WORKOUTS_COUNT, 1.0, ws.getId());
            saveActivityIfAbsent(ws.getUser(), day, MetricType.ACTIVE_MINUTES, (double) minutes, ws.getId());
        }
    }

    private void seedRichActivities(User user, int workoutDaysInPeriod, int avgMinutes) {
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 45; d++) {
            LocalDate date = today.minusDays(d);
            boolean workoutDay = (d % (45 / Math.max(workoutDaysInPeriod, 1))) == 0 || d % 4 == 0;
            if (!workoutDay) {
                continue;
            }
            long sourceId = user.getId() * 1000L + d;
            double sessions = d % 7 == 0 ? 2.0 : 1.0;
            saveActivityIfAbsent(user, date, MetricType.SESSIONS, sessions, sourceId);
            saveActivityIfAbsent(user, date, MetricType.WORKOUTS_COUNT, sessions, sourceId + 1);
            double minutes = avgMinutes + (d % 5) * 3.0;
            saveActivityIfAbsent(user, date, MetricType.ACTIVE_MINUTES, minutes, sourceId + 2);
        }
    }

    private void saveActivityIfAbsent(User user, LocalDate date, MetricType metric, double value, Long sourceId) {
        if (sourceId != null && activityRepository.existsBySourceIdAndMetricType(sourceId, metric)) {
            return;
        }
        activityRepository.save(UserActivity.builder()
                .user(user)
                .activityDate(date)
                .metricType(metric)
                .metricValue(value)
                .sourceId(sourceId)
                .build());
    }

    private void ensureDemoCompetitions(User admin, OrganizationalGroup carrera,
                                        OrganizationalGroup grupoManana, OrganizationalGroup grupoTarde,
                                        User marina, User carlos, User pedro, User sofia) {
        LocalDate today = LocalDate.now();

        updateLegacyCompetitionDates(today);

        Competition groupChallenge = Competition.builder()
                .name(MARKER_COMPETITION)
                .competitionType(CompetitionType.CHALLENGE)
                .scopeLevel(ScopeLevel.CARRERA)
                .scopeReference(carrera)
                .metricType(MetricType.WORKOUTS_COUNT)
                .startDate(today.minusDays(21))
                .endDate(today.plusDays(14))
                .status(CompetitionStatus.ACTIVE)
                .createdBy(admin)
                .build();
        competitionRepository.save(groupChallenge);
        enrollGroups(groupChallenge, carrera, grupoManana, grupoTarde);

        Competition minutesRanking = Competition.builder()
                .name("Ranking minutos — Carrera")
                .competitionType(CompetitionType.RANKING)
                .scopeLevel(ScopeLevel.CARRERA)
                .scopeReference(carrera)
                .metricType(MetricType.ACTIVE_MINUTES)
                .startDate(today.minusDays(60))
                .endDate(today.minusDays(1))
                .status(CompetitionStatus.FINISHED)
                .createdBy(admin)
                .build();
        competitionRepository.save(minutesRanking);
        enrollGroups(minutesRanking, carrera, grupoManana, grupoTarde);

        Competition individual = Competition.builder()
                .name("Duelo interno Grupo Mañana")
                .competitionType(CompetitionType.CHALLENGE)
                .scopeLevel(ScopeLevel.GRUPO)
                .scopeReference(grupoManana)
                .metricType(MetricType.SESSIONS)
                .startDate(today.minusDays(14))
                .endDate(today.plusDays(7))
                .status(CompetitionStatus.ACTIVE)
                .createdBy(admin)
                .build();
        competitionRepository.save(individual);
        enrollMembers(individual, marina, carlos, pedro, sofia);

        recalculateScores(groupChallenge);
        recalculateScores(minutesRanking);
        recalculateScores(individual);
        competitionRepository.saveAll(List.of(groupChallenge, minutesRanking, individual));
    }

    private void recalculateScores(Competition c) {
        LocalDate start = c.getStartDate();
        LocalDate end = effectiveEndDate(c);

        if (c.getScopeLevel() == ScopeLevel.GRUPO) {
            List<CompetitionMemberParticipant> members =
                    competitionMemberParticipantRepository.findLeaderboard(c.getId());
            int rank = 1;
            for (CompetitionMemberParticipant p : members) {
                Double score = activityRepository.sumUserScore(
                        p.getUser().getId(), c.getMetricType(), start, end);
                p.setScore(score != null ? score : 0.0);
                p.setRank(rank++);
                p.setLastCalculatedAt(LocalDateTime.now());
            }
            competitionMemberParticipantRepository.saveAll(members);
        } else {
            List<CompetitionParticipant> participants =
                    competitionParticipantRepository.findLeaderboard(c.getId());
            int rank = 1;
            for (CompetitionParticipant p : participants) {
                Double score = activityRepository.sumGroupScore(
                        p.getGroup().getId(), c.getMetricType().name(), start, end);
                p.setGroupScore(score != null ? score : 0.0);
                p.setRank(rank++);
                p.setLastCalculatedAt(LocalDateTime.now());
            }
            competitionParticipantRepository.saveAll(participants);
        }
    }

    private LocalDate effectiveEndDate(Competition c) {
        if (c.getEndDate() == null) {
            return LocalDate.now();
        }
        if (c.getStatus() == CompetitionStatus.FINISHED) {
            return c.getEndDate();
        }
        return c.getEndDate().isBefore(LocalDate.now()) ? c.getEndDate() : LocalDate.now();
    }

    private void updateLegacyCompetitionDates(LocalDate today) {
        competitionRepository.findAll().forEach(c -> {
            if ("Reto Marzo 2026".equals(c.getName()) && c.getStatus() == CompetitionStatus.ACTIVE) {
                c.setStartDate(today.minusDays(30));
                c.setEndDate(today.plusDays(30));
                competitionRepository.save(c);
                fixGroupParticipants(c, groupRepository.findByCode("CARR-KINE").orElse(null),
                        groupRepository.findByCode("GRP-MANANA").orElse(null),
                        groupRepository.findByCode("GRP-TARDE").orElse(null));
                removeMemberParticipants(c.getId());
                recalculateScores(c);
            }
            if ("Ranking Q1".equals(c.getName())) {
                c.setStartDate(today.minusDays(90));
                c.setEndDate(today.minusDays(7));
                c.setStatus(CompetitionStatus.FINISHED);
                c.setMetricType(MetricType.ACTIVE_MINUTES);
                competitionRepository.save(c);
                removeMemberParticipants(c.getId());
                OrganizationalGroup carrera = groupRepository.findByCode("CARR-KINE").orElse(null);
                OrganizationalGroup manana = groupRepository.findByCode("GRP-MANANA").orElse(null);
                OrganizationalGroup tarde = groupRepository.findByCode("GRP-TARDE").orElse(null);
                if (carrera != null) {
                    fixGroupParticipants(c, carrera, manana, tarde);
                }
                recalculateScores(c);
            }
        });
    }

    private void fixGroupParticipants(Competition c, OrganizationalGroup... groups) {
        List<CompetitionParticipant> existing = competitionParticipantRepository
                .findAll().stream()
                .filter(p -> p.getCompetition().getId().equals(c.getId()))
                .toList();
        if (existing.size() >= 2) {
            return;
        }
        competitionParticipantRepository.deleteAll(existing);
        enrollGroups(c, groups);
    }

    private void removeMemberParticipants(Long competitionId) {
        List<CompetitionMemberParticipant> members = competitionMemberParticipantRepository
                .findAll().stream()
                .filter(m -> m.getCompetition().getId().equals(competitionId))
                .toList();
        competitionMemberParticipantRepository.deleteAll(members);
    }

    private void enrollGroups(Competition competition, OrganizationalGroup... groups) {
        List<CompetitionParticipant> list = new ArrayList<>();
        for (OrganizationalGroup g : groups) {
            if (g == null) continue;
            boolean exists = competitionParticipantRepository
                    .existsByCompetitionIdAndGroupId(competition.getId(), g.getId());
            if (!exists) {
                list.add(CompetitionParticipant.builder()
                        .competition(competition)
                        .group(g)
                        .groupScore(0.0)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            competitionParticipantRepository.saveAll(list);
        }
    }

    private void enrollMembers(Competition competition, User... users) {
        List<CompetitionMemberParticipant> list = new ArrayList<>();
        for (User u : users) {
            if (u == null) continue;
            boolean exists = competitionMemberParticipantRepository
                    .existsByCompetitionIdAndUserId(competition.getId(), u.getId());
            if (!exists) {
                list.add(CompetitionMemberParticipant.builder()
                        .competition(competition)
                        .user(u)
                        .score(0.0)
                        .build());
            }
        }
        if (!list.isEmpty()) {
            competitionMemberParticipantRepository.saveAll(list);
        }
    }
}
