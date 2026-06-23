package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.*;
import com.example.ironplan.rest.dto.CompetitionDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetitionPodiumService {

    private static final double WEIGHT_CONSISTENCY = 0.35;
    private static final double WEIGHT_ONE_RM = 0.30;
    private static final double WEIGHT_VOLUME = 0.25;

    private final CompetitionRepository competitionRepo;
    private final CompetitionMemberParticipantRepository memberParticipantRepo;
    private final CompetitionPodiumEntryRepository podiumEntryRepo;
    private final CompetitionDeclaredWinnerRepository declaredWinnerRepo;
    private final UserActivityRepository activityRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final ExperimentoRetoRepository experimentoRetoRepo;
    private final ParticipanteRetoRepository participanteRetoRepo;
    private final OrganizationalAccessService accessService;
    private final NotificationService notificationService;

    @Transactional
    public CompetitionDTOs.PodiumsResponse generatePodiums(Long competitionId, User admin) {
        Competition competition = findCompetition(competitionId);
        requireManage(competition, admin);
        requireFinished(competition);
        requireMemberCompetition(competition);

        if (declaredWinnerRepo.findByCompetitionIdOrderByScopeAscLevelCategoryAsc(competitionId).size() > 0) {
            throw new IllegalStateException(
                    "No se pueden regenerar podios después de declarar ganadores");
        }

        podiumEntryRepo.deleteAllByCompetitionId(competitionId);

        LocalDate start = competition.getStartDate();
        LocalDate end = competition.getEndDate() != null ? competition.getEndDate() : LocalDate.now();

        List<User> participants = memberParticipantRepo.findLeaderboard(competitionId).stream()
                .map(CompetitionMemberParticipant::getUser)
                .toList();

        Map<Long, ParticipanteCategoria> categoryByUser = resolveCategories(competition, participants);

        List<UserMetricSnapshot> snapshots = participants.stream()
                .map(u -> buildSnapshot(u, start, end))
                .toList();

        LocalDateTime generatedAt = LocalDateTime.now();
        persistPool(competition, participants, snapshots, PodiumScope.GENERAL, null, generatedAt);

        for (ParticipanteCategoria level : ParticipanteCategoria.values()) {
            List<UserMetricSnapshot> pool = snapshots.stream()
                    .filter(s -> categoryByUser.get(s.userId()) == level)
                    .toList();
            if (!pool.isEmpty()) {
                persistPool(competition, participants, pool, PodiumScope.LEVEL, level, generatedAt);
            }
        }

        return getPodiums(competitionId);
    }

    @Transactional(readOnly = true)
    public CompetitionDTOs.PodiumsResponse getPodiums(Long competitionId) {
        findCompetition(competitionId);
        List<CompetitionPodiumEntry> entries =
                podiumEntryRepo.findByCompetitionIdOrderByScopeAscLevelCategoryAscRankPositionAsc(competitionId);

        List<CompetitionDTOs.PodiumEntryDto> generalTop3 = entries.stream()
                .filter(e -> e.getScope() == PodiumScope.GENERAL)
                .map(this::toPodiumDto)
                .toList();

        Map<String, List<CompetitionDTOs.PodiumEntryDto>> byLevel = new LinkedHashMap<>();
        for (ParticipanteCategoria cat : ParticipanteCategoria.values()) {
            List<CompetitionDTOs.PodiumEntryDto> levelEntries = entries.stream()
                    .filter(e -> e.getScope() == PodiumScope.LEVEL && e.getLevelCategory() == cat)
                    .map(this::toPodiumDto)
                    .toList();
            byLevel.put(cat.name(), levelEntries);
        }

        return CompetitionDTOs.PodiumsResponse.builder()
                .generated(entries.stream().findFirst().map(CompetitionPodiumEntry::getGeneratedAt).orElse(null))
                .generalTop3(generalTop3)
                .byLevel(byLevel)
                .build();
    }

    @Transactional
    public CompetitionDTOs.DeclaredWinnerDto declareWinner(
            Long competitionId,
            CompetitionDTOs.DeclareWinnerRequest request,
            User admin
    ) {
        Competition competition = findCompetition(competitionId);
        requireManage(competition, admin);
        requireFinished(competition);
        requireMemberCompetition(competition);

        PodiumScope scope = request.getScope();
        ParticipanteCategoria levelCategory = request.getLevelCategory();

        if (scope == PodiumScope.GENERAL) {
            levelCategory = null;
        } else if (levelCategory == null) {
            throw new IllegalArgumentException("levelCategory es requerido para podio por nivel");
        }

        if (scope == PodiumScope.GENERAL) {
            if (declaredWinnerRepo.findByCompetitionIdAndScopeAndLevelCategoryIsNull(
                    competitionId, PodiumScope.GENERAL).isPresent()) {
                throw new IllegalStateException("Ya hay un ganador general declarado");
            }
        } else if (declaredWinnerRepo.findByCompetitionIdAndScopeAndLevelCategory(
                competitionId, PodiumScope.LEVEL, levelCategory).isPresent()) {
            throw new IllegalStateException("Ya hay un ganador declarado para esta categoría");
        }

        if (!isUserInTop3(competitionId, scope, levelCategory, request.getUserId())) {
            throw new IllegalArgumentException("El usuario debe estar en el top 3 del podio correspondiente");
        }

        User winner = memberParticipantRepo.findLeaderboard(competitionId).stream()
                .map(CompetitionMemberParticipant::getUser)
                .filter(u -> u.getId().equals(request.getUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no participa en este reto"));

        CompetitionDeclaredWinner declared = CompetitionDeclaredWinner.builder()
                .competition(competition)
                .user(winner)
                .scope(scope)
                .levelCategory(levelCategory)
                .declaredAt(LocalDateTime.now())
                .declaredBy(admin)
                .build();
        declaredWinnerRepo.save(declared);

        String levelLabel = scope == PodiumScope.GENERAL
                ? "General"
                : levelCategoryLabel(levelCategory);

        notificationService.notifyCompetitionWinner(
                winner,
                competition.getName(),
                competition.getId(),
                scope,
                levelLabel
        );

        return toDeclaredWinnerDto(declared);
    }

    @Transactional(readOnly = true)
    public List<CompetitionDTOs.DeclaredWinnerDto> getDeclaredWinners(Long competitionId) {
        findCompetition(competitionId);
        return declaredWinnerRepo.findByCompetitionIdOrderByScopeAscLevelCategoryAsc(competitionId).stream()
                .map(this::toDeclaredWinnerDto)
                .toList();
    }

    private void persistPool(
            Competition competition,
            List<User> participants,
            List<UserMetricSnapshot> pool,
            PodiumScope scope,
            ParticipanteCategoria levelCategory,
            LocalDateTime generatedAt
    ) {
        List<ScoredSnapshot> scored = scorePool(pool);
        int rank = 1;
        for (ScoredSnapshot s : scored.stream().limit(3).toList()) {
            UserMetricSnapshot snap = s.snapshot();
            CompetitionPodiumEntry entry = CompetitionPodiumEntry.builder()
                    .competition(competition)
                    .user(findUserById(snap.userId(), participants))
                    .scope(scope)
                    .levelCategory(levelCategory)
                    .rankPosition(rank++)
                    .compositeScore(s.compositeScore())
                    .consistencyRaw(snap.consistencyRaw())
                    .oneRmProgressRaw(snap.oneRmProgressRaw())
                    .volumeRaw(snap.volumeRaw())
                    .consistencyNorm(s.consistencyNorm())
                    .oneRmNorm(s.oneRmNorm())
                    .volumeNorm(s.volumeNorm())
                    .generatedAt(generatedAt)
                    .build();
            podiumEntryRepo.save(entry);
        }
    }

    private User findUserById(Long userId, List<User> participants) {
        return participants.stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    private List<ScoredSnapshot> scorePool(List<UserMetricSnapshot> pool) {
        if (pool.isEmpty()) return List.of();

        double minC = pool.stream().mapToDouble(UserMetricSnapshot::consistencyRaw).min().orElse(0);
        double maxC = pool.stream().mapToDouble(UserMetricSnapshot::consistencyRaw).max().orElse(0);
        double minO = pool.stream().mapToDouble(UserMetricSnapshot::oneRmProgressRaw).min().orElse(0);
        double maxO = pool.stream().mapToDouble(UserMetricSnapshot::oneRmProgressRaw).max().orElse(0);
        double minV = pool.stream().mapToDouble(UserMetricSnapshot::volumeRaw).min().orElse(0);
        double maxV = pool.stream().mapToDouble(UserMetricSnapshot::volumeRaw).max().orElse(0);

        List<ScoredSnapshot> scored = new ArrayList<>();
        for (UserMetricSnapshot snap : pool) {
            double cNorm = normalize(snap.consistencyRaw(), minC, maxC);
            double oNorm = normalize(snap.oneRmProgressRaw(), minO, maxO);
            double vNorm = normalize(snap.volumeRaw(), minV, maxV);
            double composite = (WEIGHT_CONSISTENCY * cNorm + WEIGHT_ONE_RM * oNorm + WEIGHT_VOLUME * vNorm) * 100;
            scored.add(new ScoredSnapshot(snap, composite, cNorm, oNorm, vNorm));
        }

        scored.sort(Comparator.comparingDouble(ScoredSnapshot::compositeScore).reversed());
        return scored;
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) return value > 0 ? 1.0 : 0.0;
        return (value - min) / (max - min);
    }

    private UserMetricSnapshot buildSnapshot(User user, LocalDate start, LocalDate end) {
        Double workouts = activityRepo.sumUserScore(user.getId(), MetricType.WORKOUTS_COUNT, start, end);
        Double freeActivity = activityRepo.sumUserScore(user.getId(), MetricType.FREE_ACTIVITY_COUNT, start, end);
        Double volume = activityRepo.sumUserScore(user.getId(), MetricType.VOLUME_TOTAL, start, end);

        double consistency = (workouts != null ? workouts : 0) + (freeActivity != null ? freeActivity : 0);
        double volumeRaw = volume != null ? volume : 0;
        double oneRmProgress = calculateOneRmProgress(user.getId(), start, end);

        return new UserMetricSnapshot(user.getId(), consistency, oneRmProgress, volumeRaw);
    }

    private double calculateOneRmProgress(Long userId, LocalDate start, LocalDate end) {
        long totalDays = ChronoUnit.DAYS.between(start, end);
        LocalDate mid = totalDays > 0 ? start.plusDays(totalDays / 2) : start;

        BigDecimal firstHalfMax = workoutSetRepo.maxOneRmInPeriod(userId, start, mid);
        LocalDate secondHalfStart = mid.plusDays(totalDays > 0 ? 1 : 0);
        if (secondHalfStart.isAfter(end)) {
            secondHalfStart = mid;
        }
        BigDecimal secondHalfMax = workoutSetRepo.maxOneRmInPeriod(userId, secondHalfStart, end);

        double baseline;
        if (firstHalfMax != null) {
            baseline = firstHalfMax.doubleValue();
        } else {
            List<BigDecimal> chronological = workoutSetRepo.findOneRmValuesChronological(userId, start, end);
            baseline = chronological.isEmpty() ? 0 : chronological.get(0).doubleValue();
        }

        if (baseline <= 0 || secondHalfMax == null) return 0;
        double endMax = secondHalfMax.doubleValue();
        if (endMax <= baseline) return 0;
        return ((endMax - baseline) / baseline) * 100.0;
    }

    private Map<Long, ParticipanteCategoria> resolveCategories(Competition competition, List<User> participants) {
        Map<Long, ParticipanteCategoria> map = new HashMap<>();
        Optional<ExperimentoReto> retoOpt = experimentoRetoRepo.findByCompetitionId(competition.getId());

        for (User user : participants) {
            if (retoOpt.isPresent()) {
                participanteRetoRepo.findByRetoIdAndUsuarioId(retoOpt.get().getId(), user.getId())
                        .ifPresent(p -> map.put(user.getId(), p.getCategoria()));
            }
            map.putIfAbsent(user.getId(), mapLevelToCategory(user.getLevel()));
        }
        return map;
    }

    private ParticipanteCategoria mapLevelToCategory(Level level) {
        if (level == null) return ParticipanteCategoria.PRINCIPIANTE;
        return switch (level) {
            case NOVATO -> ParticipanteCategoria.PRINCIPIANTE;
            case INTERMEDIO -> ParticipanteCategoria.INTERMEDIO;
            case AVANZADO -> ParticipanteCategoria.AVANZADO;
        };
    }

    private boolean isUserInTop3(Long competitionId, PodiumScope scope,
                                   ParticipanteCategoria levelCategory, Long userId) {
        List<CompetitionPodiumEntry> entries = scope == PodiumScope.GENERAL
                ? podiumEntryRepo.findByCompetitionIdAndScopeOrderByRankPositionAsc(competitionId, PodiumScope.GENERAL)
                : podiumEntryRepo.findByCompetitionIdAndScopeAndLevelCategoryOrderByRankPositionAsc(
                        competitionId, PodiumScope.LEVEL, levelCategory);

        return entries.stream()
                .anyMatch(e -> e.getUser().getId().equals(userId) && e.getRankPosition() <= 3);
    }

    private CompetitionDTOs.PodiumEntryDto toPodiumDto(CompetitionPodiumEntry e) {
        User u = e.getUser();
        return CompetitionDTOs.PodiumEntryDto.builder()
                .rank(e.getRankPosition())
                .userId(u.getId())
                .fullName(buildFullName(u))
                .username(u.getDisplayUsername())
                .profilePictureUrl(u.getProfilePictureUrl())
                .levelCategory(e.getLevelCategory() != null ? e.getLevelCategory().name() : null)
                .compositeScore(e.getCompositeScore())
                .consistencyRaw(e.getConsistencyRaw())
                .oneRmProgressRaw(e.getOneRmProgressRaw())
                .volumeRaw(e.getVolumeRaw())
                .consistencyNorm(e.getConsistencyNorm())
                .oneRmNorm(e.getOneRmNorm())
                .volumeNorm(e.getVolumeNorm())
                .build();
    }

    private CompetitionDTOs.DeclaredWinnerDto toDeclaredWinnerDto(CompetitionDeclaredWinner w) {
        User u = w.getUser();
        String levelLabel = w.getScope() == PodiumScope.GENERAL
                ? "General"
                : levelCategoryLabel(w.getLevelCategory());
        return CompetitionDTOs.DeclaredWinnerDto.builder()
                .scope(w.getScope().name())
                .levelCategory(w.getLevelCategory() != null ? w.getLevelCategory().name() : null)
                .levelLabel(levelLabel)
                .userId(u.getId())
                .fullName(buildFullName(u))
                .username(u.getDisplayUsername())
                .profilePictureUrl(u.getProfilePictureUrl())
                .declaredAt(w.getDeclaredAt())
                .build();
    }

    private String levelCategoryLabel(ParticipanteCategoria cat) {
        if (cat == null) return "General";
        return switch (cat) {
            case PRINCIPIANTE -> "Principiante";
            case INTERMEDIO -> "Intermedio";
            case AVANZADO -> "Avanzado";
        };
    }

    private String buildFullName(User u) {
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getDisplayUsername() : full;
    }

    private Competition findCompetition(Long id) {
        return competitionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Competencia no encontrada: " + id));
    }

    private void requireFinished(Competition c) {
        if (c.getStatus() != CompetitionStatus.FINISHED) {
            throw new IllegalStateException("Solo se pueden gestionar podios en retos finalizados");
        }
    }

    private void requireMemberCompetition(Competition c) {
        if (c.getScopeLevel() != ScopeLevel.GRUPO
                && c.getParticipantMode() != ParticipantMode.ORGANIZATION_MEMBERS) {
            throw new IllegalStateException("Los podios compuestos solo aplican a retos individuales");
        }
    }

    private void requireManage(Competition competition, User admin) {
        if (competition.getScopeReference() != null) {
            accessService.requireManage(competition.getScopeReference());
        }
    }

    private record UserMetricSnapshot(
            Long userId,
            double consistencyRaw,
            double oneRmProgressRaw,
            double volumeRaw
    ) {}

    private record ScoredSnapshot(
            UserMetricSnapshot snapshot,
            double compositeScore,
            double consistencyNorm,
            double oneRmNorm,
            double volumeNorm
    ) {}
}
