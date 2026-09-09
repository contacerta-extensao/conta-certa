package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.gamification.AchievementResponse;
import com.ifsc.contacerta.dto.gamification.RankingResponse;
import com.ifsc.contacerta.entity.AchievementUnlock;
import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.entity.RoomMembership;
import com.ifsc.contacerta.entity.RoomStudentProgress;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AchievementCode;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.AchievementUnlockRepository;
import com.ifsc.contacerta.repository.RankingRepository;
import com.ifsc.contacerta.repository.RankingRowProjection;
import com.ifsc.contacerta.repository.RoomMembershipRepository;
import com.ifsc.contacerta.repository.RoomStudentProgressRepository;
import com.ifsc.contacerta.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentGamificationServiceTest {

	private UserRepository userRepository;
	private RoomMembershipRepository membershipRepository;
	private RoomStudentProgressRepository progressRepository;
	private RankingRepository rankingRepository;
	private AchievementUnlockRepository unlockRepository;
	private StudentGamificationService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		membershipRepository = mock(RoomMembershipRepository.class);
		progressRepository = mock(RoomStudentProgressRepository.class);
		rankingRepository = mock(RankingRepository.class);
		unlockRepository = mock(AchievementUnlockRepository.class);
		service = new StudentGamificationService(
				userRepository, membershipRepository, progressRepository, rankingRepository, unlockRepository
		);
	}

	@Test
	void deveAnonimizarRankingEIncluirPosicaoPropriaForaDaPagina() {
		Fixture fixture = fixture();
		RankingRowProjection peer = row(1, UUID.randomUUID(), " Ana   Beatriz Souza ", 500, 9, 6);
		RankingRowProjection self = row(37, fixture.student().getId(), "Luiz Mendes", 120, 3, 2);
		when(rankingRepository.findPage(fixture.room().getId(), PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(peer), PageRequest.of(0, 20), 48));
		when(rankingRepository.findStudent(fixture.room().getId(), fixture.student().getId()))
				.thenReturn(Optional.of(self));

		RankingResponse response = service.ranking(fixture.student().getId(), fixture.room().getId(), 0, 20);

		assertThat(response.content()).singleElement().satisfies(entry -> {
			assertThat(entry.displayName()).isEqualTo("Ana S.");
			assertThat(entry.me()).isFalse();
			assertThat(entry.xp()).isEqualTo(500);
			assertThat(entry.stars()).isEqualTo(9);
			assertThat(entry.lessonsPassed()).isEqualTo(6);
		});
		assertThat(response.me().position()).isEqualTo(37);
		assertThat(response.me().displayName()).isEqualTo("Luiz M.");
		assertThat(response.me().me()).isTrue();
		assertThat(response.totalElements()).isEqualTo(48);
		assertThat(response.totalPages()).isEqualTo(3);
	}

	@Test
	void deveRetornarCatalogoCompletoComProgressoLimitado() {
		Fixture fixture = fixture();
		RoomStudentProgress progress = new RoomStudentProgress(fixture.room(), fixture.student());
		progress.applyResult(700, 0, false, false, Instant.parse("2026-08-29T10:00:00Z"));
		for (int index = 0; index < 7; index++) {
			progress.applyResult(0, 0, false, true, Instant.parse("2026-08-29T10:00:00Z"));
		}
		Instant unlockedAt = Instant.parse("2026-08-29T11:00:00Z");
		when(progressRepository.findByRoomIdAndStudentId(fixture.room().getId(), fixture.student().getId()))
				.thenReturn(Optional.of(progress));
		when(unlockRepository.findByRoomIdAndStudentId(fixture.room().getId(), fixture.student().getId()))
				.thenReturn(List.of(new AchievementUnlock(
						fixture.room(), fixture.student(), AchievementCode.PERFECT_SCORE, unlockedAt
				)));

		List<AchievementResponse> response = service.achievements(
				fixture.student().getId(), fixture.room().getId()
		);

		assertThat(response).extracting(item -> item.code().name())
				.containsExactly("FIRST_PASS", "PERFECT_SCORE", "XP_100", "XP_500", "XP_1000", "PASSED_5", "PASSED_10");
		assertThat(response).allSatisfy(item -> assertThat(item.icon()).isNotBlank());
		assertThat(response).filteredOn(item -> item.code() == AchievementCode.PERFECT_SCORE)
				.singleElement().satisfies(item -> {
					assertThat(item.progressCurrent()).isEqualTo(1);
					assertThat(item.progressTarget()).isEqualTo(1);
					assertThat(item.unlockedAt()).isEqualTo(unlockedAt);
				});
		assertThat(response).filteredOn(item -> item.code() == AchievementCode.XP_500)
				.singleElement().satisfies(item -> assertThat(item.progressCurrent()).isEqualTo(500));
		assertThat(response).filteredOn(item -> item.code() == AchievementCode.PASSED_5)
				.singleElement().satisfies(item -> assertThat(item.progressCurrent()).isEqualTo(5));
	}

	@Test
	void deveRetornarProgressoZeroSemProjecao() {
		Fixture fixture = fixture();

		List<AchievementResponse> response = service.achievements(
				fixture.student().getId(), fixture.room().getId()
		);

		assertThat(response).allSatisfy(item -> {
			assertThat(item.progressCurrent()).isZero();
			assertThat(item.unlocked()).isFalse();
			assertThat(item.unlockedAt()).isNull();
		});
	}

	@Test
	void deveOcultarSalaSemMatriculaAtiva() {
		Fixture fixture = fixture(false);

		assertThatThrownBy(() -> service.ranking(fixture.student().getId(), fixture.room().getId(), 0, 20))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(exception.getCode()).isEqualTo("ROOM_NOT_FOUND");
				});
	}

	private Fixture fixture() {
		return fixture(true);
	}

	private Fixture fixture(boolean activeMembership) {
		Institution institution = new Institution(
				"Instituto", "11222333000181", "contato@example.com", "48999990000", true
		);
		User teacher = new User(Role.TEACHER, AccountStatus.ACTIVE, "Professora", "prof@example.com", "P-1", institution);
		User student = new User(Role.STUDENT, AccountStatus.ACTIVE, "Luiz Mendes", "aluno@example.com", "A-1", institution);
		Room room = new Room(
				"Sala", null, Grade.HIGH_SCHOOL_1, List.of("Porcentagem"), 50,
				"ABC234", "hash", teacher, institution
		);
		when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
		if (activeMembership) {
			when(membershipRepository.findByRoomIdAndStudentId(room.getId(), student.getId()))
					.thenReturn(Optional.of(new RoomMembership(room, student)));
		}
		return new Fixture(room, student);
	}

	private RankingRowProjection row(long position, UUID id, String name, int xp, int stars, int lessonsPassed) {
		RankingRowProjection row = mock(RankingRowProjection.class);
		when(row.getPosition()).thenReturn(position);
		when(row.getStudentId()).thenReturn(id);
		when(row.getFullName()).thenReturn(name);
		when(row.getXp()).thenReturn(xp);
		when(row.getStars()).thenReturn(stars);
		when(row.getLessonsPassed()).thenReturn(lessonsPassed);
		return row;
	}

	private record Fixture(Room room, User student) {}
}
