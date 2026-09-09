package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.gamification.AchievementResponse;
import com.ifsc.contacerta.dto.gamification.RankingEntryResponse;
import com.ifsc.contacerta.dto.gamification.RankingResponse;
import com.ifsc.contacerta.dto.studentdashboard.StudentFinancialTipResponse;
import com.ifsc.contacerta.dto.studentdashboard.StudentRoomDashboardResponse;
import com.ifsc.contacerta.dto.studentdashboard.StudentNextLessonResponse;
import com.ifsc.contacerta.dto.studentlesson.LessonRulesResponse;
import com.ifsc.contacerta.dto.studentlesson.StudentLessonPathResponse;
import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.entity.RoomMembership;
import com.ifsc.contacerta.entity.RoomStudentProgress;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AchievementCode;
import com.ifsc.contacerta.model.AttemptAvailabilityStatus;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.RoomMembershipRepository;
import com.ifsc.contacerta.repository.RoomStudentProgressRepository;
import com.ifsc.contacerta.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentRoomDashboardServiceTest {

	@Mock private UserRepository userRepository;
	@Mock private RoomMembershipRepository membershipRepository;
	@Mock private RoomStudentProgressRepository progressRepository;
	@Mock private StudentLessonService lessonService;
	@Mock private StudentGamificationService gamificationService;
	@Mock private StudentFinancialTipService financialTipService;

	private StudentRoomDashboardService service;
	private User student;
	private Room room;
	private RoomMembership membership;

	@BeforeEach
	void setUp() {
		service = new StudentRoomDashboardService(
				userRepository, membershipRepository, progressRepository, lessonService, gamificationService, financialTipService
		);
		Institution institution = new Institution("IFSC", "11222333000181", "ifsc@example.com", "+5548999999999", true);
		User teacher = new User(Role.TEACHER, AccountStatus.ACTIVE, "Professora Ana", "ana@example.com", null, institution);
		student = new User(Role.STUDENT, AccountStatus.ACTIVE, "Aluno Silva", "aluno@example.com", "2026001", institution);
		room = new Room("Sala A", "Descrição", Grade.HIGH_SCHOOL_1, List.of("Juros"), 60, "ABC123", "hash", teacher, institution);
		membership = new RoomMembership(room, student);
	}

	@Test
	void deveAgregarProgressoTrilhaConquistasRankingEDica() {
		RoomStudentProgress progress = new RoomStudentProgress(room, student);
		progress.applyResult(100, 3, true, true, Instant.parse("2026-09-01T10:00:00Z"));
		progress.applyResult(50, 2, true, true, Instant.parse("2026-09-02T10:00:00Z"));
		progress.applyResult(0, 0, true, false, Instant.parse("2026-09-03T10:00:00Z"));
		StudentLessonPathResponse available = lesson("Disponível", 1, AttemptAvailabilityStatus.AVAILABLE, 3L);
		StudentLessonPathResponse inProgress = lesson("Em andamento", 2, AttemptAvailabilityStatus.IN_PROGRESS, 2L);
		StudentLessonPathResponse retry = lesson("Tentativa", 3, AttemptAvailabilityStatus.FAILED, null);
		RankingEntryResponse me = new RankingEntryResponse(2, student.getId(), "Aluno S.", 150, 5, 2, true);
		StudentFinancialTipResponse tip = new StudentFinancialTipResponse(
				UUID.randomUUID(), "Reserve", "Conteúdo", "https://example.com", LocalDate.of(2026, 9, 4)
		);
		stubAuthorizedAccess();
		when(progressRepository.findByRoomIdAndStudentId(room.getId(), student.getId())).thenReturn(Optional.of(progress));
		when(lessonService.path(student.getId(), room.getId())).thenReturn(List.of(available, inProgress, retry));
		when(gamificationService.achievements(student.getId(), room.getId())).thenReturn(List.of(
				achievement(AchievementCode.XP_100, true, Instant.parse("2026-09-01T00:00:00Z")),
				achievement(AchievementCode.FIRST_PASS, true, Instant.parse("2026-09-03T00:00:00Z")),
				achievement(AchievementCode.PASSED_5, true, Instant.parse("2026-09-02T00:00:00Z")),
				achievement(AchievementCode.XP_500, true, Instant.parse("2026-09-01T00:00:00Z")),
				achievement(AchievementCode.XP_1000, false, null)
		));
		when(gamificationService.ranking(student.getId(), room.getId(), 0, 1))
				.thenReturn(new RankingResponse(List.of(me), me, 0, 1, 12, 12));
		when(financialTipService.currentTip()).thenReturn(tip);

		StudentRoomDashboardResponse response = service.dashboard(student.getId(), room.getId());

		assertThat(response.room().progressPercent()).isEqualTo(66);
		assertThat(response).extracting(
				StudentRoomDashboardResponse::progressPercent,
				StudentRoomDashboardResponse::level,
				StudentRoomDashboardResponse::xpTotal,
				StudentRoomDashboardResponse::levelProgressPercent,
				StudentRoomDashboardResponse::xpToNextLevel,
				StudentRoomDashboardResponse::starsTotal,
				StudentRoomDashboardResponse::starsPossible,
				StudentRoomDashboardResponse::lessonsCompleted,
				StudentRoomDashboardResponse::lessonsTotal,
				StudentRoomDashboardResponse::rankingPosition,
				StudentRoomDashboardResponse::rankingParticipants
		).containsExactly(66, 2, 150, 50, 50, 5, 9, 2, 3, 2L, 12L);
		assertThat(response.nextLesson()).isEqualTo(new StudentNextLessonResponse(
				inProgress.assignmentId(), inProgress.lessonId(), inProgress.title(), inProgress.order(), null
		));
		assertThat(response.recentAchievements()).extracting(AchievementResponse::code)
				.containsExactly(AchievementCode.FIRST_PASS, AchievementCode.PASSED_5, AchievementCode.XP_100);
		assertThat(response.tipOfDay()).isEqualTo(tip);
	}

	@Test
	void deveUsarValoresPadraoQuandoNaoExisteProgressoENaoHaLicoesContinuaveis() {
		stubAuthorizedAccess();
		when(progressRepository.findByRoomIdAndStudentId(room.getId(), student.getId())).thenReturn(Optional.empty());
		when(lessonService.path(student.getId(), room.getId())).thenReturn(List.of(
				lesson("Bloqueada", 1, AttemptAvailabilityStatus.LOCKED, 0L),
				lesson("Esgotada", 2, AttemptAvailabilityStatus.FAILED, 0L)
		));
		when(gamificationService.achievements(student.getId(), room.getId())).thenReturn(List.of());
		when(gamificationService.ranking(student.getId(), room.getId(), 0, 1))
				.thenReturn(new RankingResponse(List.of(), null, 0, 1, 0, 0));

		StudentRoomDashboardResponse response = service.dashboard(student.getId(), room.getId());

		assertThat(response.room().progressPercent()).isZero();
		assertThat(response).extracting(
				StudentRoomDashboardResponse::progressPercent,
				StudentRoomDashboardResponse::level,
				StudentRoomDashboardResponse::xpTotal,
				StudentRoomDashboardResponse::levelProgressPercent,
				StudentRoomDashboardResponse::xpToNextLevel,
				StudentRoomDashboardResponse::starsTotal,
				StudentRoomDashboardResponse::starsPossible,
				StudentRoomDashboardResponse::lessonsCompleted,
				StudentRoomDashboardResponse::lessonsTotal,
				StudentRoomDashboardResponse::rankingPosition,
				StudentRoomDashboardResponse::rankingParticipants
		).containsExactly(0, 1, 0, 0, 100, 0, 6, 0, 2, null, 0L);
		assertThat(response.nextLesson()).isNull();
		assertThat(response.tipOfDay()).isNull();
	}

	@Test
	void deveRejeitarContaInativa() {
		student.deactivate();
		when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

		assertApiError(() -> service.dashboard(student.getId(), room.getId()), HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
	}

	private void stubAuthorizedAccess() {
		when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
		when(membershipRepository.findByRoomIdAndStudentId(room.getId(), student.getId())).thenReturn(Optional.of(membership));
	}

	private StudentLessonPathResponse lesson(
			String title,
			int order,
			AttemptAvailabilityStatus availability,
			Long attemptsRemaining
	) {
		return new StudentLessonPathResponse(
				UUID.randomUUID(), UUID.randomUUID(), title, null, order, availability, null, null, null,
				new LessonRulesResponse(null, attemptsRemaining == null ? null : 3, 0, attemptsRemaining, 1, 60),
				null, null, null, null, null
		);
	}

	private AchievementResponse achievement(AchievementCode code, boolean unlocked, Instant unlockedAt) {
		return new AchievementResponse(code, code.name(), "Descrição", "pi pi-star-fill", unlocked, unlockedAt, 1, 1);
	}

	private void assertApiError(Runnable action, HttpStatus status, String code) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(status);
					assertThat(exception.getCode()).isEqualTo(code);
				});
	}
}
