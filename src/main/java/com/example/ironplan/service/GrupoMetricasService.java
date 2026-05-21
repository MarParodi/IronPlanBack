package com.example.ironplan.service;

import com.example.ironplan.model.MetricType;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.OrganizationalGroupMemberRepository;
import com.example.ironplan.repository.UserActivityRepository;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.GrupoDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GrupoMetricasService {

    private final OrganizationalAccessService accessService;
    private final OrganizationalGroupMemberRepository memberRepo;
    private final UserActivityRepository activityRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public GrupoDTOs.GroupMetrics getMetrics(Long groupId, User requester, int periodDays) {
        accessService.requireManage(groupId);

        int days = Math.max(7, Math.min(periodDays, 90));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        List<Long> userIds = listUserIdsInScope(groupId);
        int totalMembers = userIds.size();

        if (userIds.isEmpty()) {
            return emptyMetrics(days, start, end);
        }

        long activeMembers = activityRepo.countDistinctActiveUsers(
            userIds, MetricType.WORKOUTS_COUNT, start, end);

        double totalWorkouts = activityRepo.sumMetricForUsers(
            userIds, MetricType.WORKOUTS_COUNT, start, end);
        double totalSessions = activityRepo.sumMetricForUsers(
            userIds, MetricType.SESSIONS, start, end);
        double totalActiveMinutes = activityRepo.sumMetricForUsers(
            userIds, MetricType.ACTIVE_MINUTES, start, end);

        double adherence = totalMembers > 0
            ? Math.round((activeMembers * 1000.0) / totalMembers) / 10.0
            : 0.0;
        double avgWorkouts = activeMembers > 0
            ? Math.round((totalWorkouts * 10.0) / activeMembers) / 10.0
            : 0.0;

        List<GrupoDTOs.WeeklyMetricPoint> weekly = buildWeeklyWorkouts(userIds, end);
        List<GrupoDTOs.ParticipantMetricRank> top = buildTopParticipants(userIds, start, end);

        return GrupoDTOs.GroupMetrics.builder()
            .periodDays(days)
            .periodStart(start)
            .periodEnd(end)
            .totalMembers(totalMembers)
            .activeMembers((int) activeMembers)
            .adherencePercent(adherence)
            .participationPercent(adherence)
            .totalWorkouts((long) totalWorkouts)
            .totalSessions((long) totalSessions)
            .totalActiveMinutes((long) totalActiveMinutes)
            .avgWorkoutsPerActiveMember(avgWorkouts)
            .weeklyWorkouts(weekly)
            .topParticipants(top)
            .build();
    }

    private List<Long> listUserIdsInScope(Long groupId) {
        List<Long> treeIds = accessService.collectTreeGroupIds(groupId);
        if (treeIds.isEmpty()) return List.of();

        Set<Long> ids = new HashSet<>(memberRepo.findDistinctActiveUserIdsByGroupIds(treeIds));
        userRepo.findByPrimaryOrganizationalGroupIdIn(treeIds).forEach(u -> ids.add(u.getId()));
        return ids.stream().toList();
    }

    private List<GrupoDTOs.WeeklyMetricPoint> buildWeeklyWorkouts(List<Long> userIds, LocalDate end) {
        LocalDate weekStart = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate rangeStart = weekStart.minusWeeks(5);
        List<Object[]> daily = activityRepo.sumDailyMetricForUsers(
            userIds, MetricType.WORKOUTS_COUNT, rangeStart, end);

        Map<LocalDate, Double> byDay = new HashMap<>();
        for (Object[] row : daily) {
            byDay.put((LocalDate) row[0], ((Number) row[1]).doubleValue());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"));
        List<GrupoDTOs.WeeklyMetricPoint> weeks = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate ws = weekStart.minusWeeks(i);
            LocalDate we = ws.plusDays(6);
            double workouts = 0;
            for (LocalDate d = ws; !d.isAfter(we); d = d.plusDays(1)) {
                workouts += byDay.getOrDefault(d, 0.0);
            }
            long activeMembers = activityRepo.countDistinctActiveUsers(userIds, MetricType.WORKOUTS_COUNT, ws, we);
            weeks.add(GrupoDTOs.WeeklyMetricPoint.builder()
                .weekStart(ws)
                .weekLabel(ws.format(fmt) + " – " + we.format(fmt))
                .workouts((long) workouts)
                .activeMembers((int) activeMembers)
                .build());
        }
        return weeks;
    }

    private List<GrupoDTOs.ParticipantMetricRank> buildTopParticipants(
            List<Long> userIds, LocalDate start, LocalDate end) {
        List<Object[]> rows = activityRepo.rankUsersByMetric(userIds, MetricType.WORKOUTS_COUNT, start, end);
        List<GrupoDTOs.ParticipantMetricRank> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            if (rank > 10) break;
            Long userId = (Long) row[0];
            double workouts = ((Number) row[1]).doubleValue();
            if (workouts <= 0) continue;

            User user = userRepo.findById(userId).orElse(null);
            if (user == null) continue;

            double minutes = activityRepo.sumMetricForUsers(
                List.of(userId), MetricType.ACTIVE_MINUTES, start, end);

            result.add(GrupoDTOs.ParticipantMetricRank.builder()
                .rank(rank++)
                .userId(userId)
                .fullName(user.getFirstName() + " " + user.getLastName())
                .username(user.getDisplayUsername())
                .profilePictureUrl(user.getProfilePictureUrl())
                .workouts((long) workouts)
                .activeMinutes((long) minutes)
                .score(workouts)
                .build());
        }
        return result;
    }

    private GrupoDTOs.GroupMetrics emptyMetrics(int days, LocalDate start, LocalDate end) {
        return GrupoDTOs.GroupMetrics.builder()
            .periodDays(days)
            .periodStart(start)
            .periodEnd(end)
            .totalMembers(0)
            .activeMembers(0)
            .adherencePercent(0)
            .participationPercent(0)
            .totalWorkouts(0)
            .totalSessions(0)
            .totalActiveMinutes(0)
            .avgWorkoutsPerActiveMember(0)
            .weeklyWorkouts(List.of())
            .topParticipants(List.of())
            .build();
    }
}
