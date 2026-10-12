package com.example.ironplan.service;

import com.example.ironplan.model.Competition;
import com.example.ironplan.model.CompetitionParticipant;
import com.example.ironplan.model.RetoTeamBonus;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.CompetitionParticipantRepository;
import com.example.ironplan.repository.RetoTeamBonusRepository;
import com.example.ironplan.rest.dto.RetoTeamBonusDTOs;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Bonos manuales que un admin asigna a un equipo en un reto TEAM_POINTS. */
@Service
@RequiredArgsConstructor
public class RetoTeamBonusService {

    private static final String EVIDENCE_FOLDER = "ironplan/reto-bonus";
    private static final int MAX_TITLE_LENGTH = 150;

    private final CompetitionService competitionService;
    private final OrganizationalAccessService accessService;
    private final CloudinaryService cloudinaryService;
    private final RetoTeamBonusRepository bonusRepo;
    private final CompetitionParticipantRepository participantRepo;

    @Transactional(readOnly = true)
    public List<RetoTeamBonusDTOs.BonusItem> list(Long competitionId) {
        competitionService.requireRetoAdminScope(competitionId);
        return bonusRepo.findByCompetition_IdOrderByCreatedAtDesc(competitionId).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public RetoTeamBonusDTOs.BonusItem create(
            Long competitionId, Long groupId, Double points, String title, MultipartFile file) {
        Competition c = competitionService.requireRetoAdminScope(competitionId).competition();
        if (groupId == null) {
            throw new IllegalArgumentException("Selecciona un equipo.");
        }
        if (points == null || !Double.isFinite(points) || points <= 0) {
            throw new IllegalArgumentException("Los puntos deben ser mayores a 0.");
        }
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("El título del bono es obligatorio.");
        }
        if (cleanTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("El título no puede superar " + MAX_TITLE_LENGTH + " caracteres.");
        }
        CompetitionParticipant participant = participantRepo.findByCompetitionIdAndGroupId(competitionId, groupId)
                .orElseThrow(() -> new IllegalArgumentException("El equipo no participa en este reto."));

        String evidenceUrl = null;
        String evidencePublicId = null;
        if (file != null && !file.isEmpty()) {
            try {
                Map<?, ?> uploaded = cloudinaryService.upload(file, EVIDENCE_FOLDER);
                evidenceUrl = String.valueOf(uploaded.get("secure_url"));
                Object publicId = uploaded.get("public_id");
                evidencePublicId = publicId != null ? publicId.toString() : null;
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo subir la imagen de prueba.", e);
            }
        }

        User current = accessService.getCurrentUser();
        RetoTeamBonus saved = bonusRepo.save(RetoTeamBonus.builder()
                .competition(c)
                .group(participant.getGroup())
                .points(points)
                .title(cleanTitle)
                .evidenceUrl(evidenceUrl)
                .evidencePublicId(evidencePublicId)
                .createdBy(current)
                .build());

        competitionService.recalculateScoresManual(competitionId);
        return toItem(saved);
    }

    @Transactional
    public void delete(Long competitionId, Long bonusId) {
        competitionService.requireRetoAdminScope(competitionId);
        RetoTeamBonus bonus = bonusRepo.findById(bonusId)
                .filter(b -> b.getCompetition().getId().equals(competitionId))
                .orElseThrow(() -> new EntityNotFoundException("Bono no encontrado."));
        bonusRepo.delete(bonus);
        bonusRepo.flush();
        competitionService.recalculateScoresManual(competitionId);
    }

    private RetoTeamBonusDTOs.BonusItem toItem(RetoTeamBonus b) {
        return RetoTeamBonusDTOs.BonusItem.builder()
                .id(b.getId())
                .groupId(b.getGroup().getId())
                .groupName(b.getGroup().getName())
                .points(b.getPoints())
                .title(b.getTitle())
                .evidenceUrl(b.getEvidenceUrl())
                .createdByName(fullName(b.getCreatedBy()))
                .createdAt(b.getCreatedAt())
                .build();
    }

    private static String fullName(User u) {
        if (u == null) return null;
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? null : name;
    }
}
