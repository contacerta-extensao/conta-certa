package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.teacher.TeacherDashboardResponse;
import com.ifsc.contacerta.dto.teacher.TeacherDashboardRoomResponse;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AttemptStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.MembershipStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.AttemptRepository;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.RoomMembershipRepository;
import com.ifsc.contacerta.repository.RoomRepository;
import com.ifsc.contacerta.repository.TeacherDashboardRoomProjection;
import com.ifsc.contacerta.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
	private static final Instant SEVEN_DAYS_AGO = Instant.parse("2026-09-01T12:00:00Z");
	private static final List<AttemptStatus> FINALIZED = List.of(AttemptStatus.SUBMITTED, AttemptStatus.EXPIRED);

	@Mock private UserRepository userRepository;
	@Mock private RoomRepository roomRepository;
	@Mock private RoomMembershipRepository membershipRepository;
	@Mock private LessonRepository lessonRepository;
	@Mock private AttemptRepository attemptRepository;
	@Spy private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	@InjectMocks private TeacherDashboardService service;

	@Test
	void deveRetornarTodasAsContagensZeradasParaProfessorSemDados() {
		User teacher = user(Role.TEACHER, AccountStatus.ACTIVE);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

		assertThat(service.get(teacher.getId())).isEqualTo(new TeacherDashboardResponse(
				0, 0, 0, 0, 0, 0, 0, 0, List.of()
		));
	}

	@Test
	void deveComporContagensPorEstadoNoEscopoDoProfessor() {
		User teacher = user(Role.TEACHER, AccountStatus.ACTIVE);
		UUID teacherId = teacher.getId();
		UUID roomId = UUID.randomUUID();
		when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
		when(roomRepository.countByTeacherId(teacherId)).thenReturn(4L);
		when(roomRepository.countByTeacherIdAndArchivedAtIsNull(teacherId)).thenReturn(3L);
		when(roomRepository.countByTeacherIdAndArchivedAtIsNotNull(teacherId)).thenReturn(1L);
		when(membershipRepository.countDistinctStudentsByTeacherId(teacherId)).thenReturn(2L);
		when(lessonRepository.countByTeacherId(teacherId)).thenReturn(12L);
		when(lessonRepository.countByTeacherIdAndStatus(teacherId, ContentStatus.PUBLISHED)).thenReturn(8L);
		when(lessonRepository.countByTeacherIdAndStatus(teacherId, ContentStatus.DRAFT)).thenReturn(3L);
		when(attemptRepository.countFinalizedByTeacherIdSince(teacherId, FINALIZED, SEVEN_DAYS_AGO)).thenReturn(17L);
		TeacherDashboardRoomProjection room = room(roomId, "2º ano A", 18, null, Instant.parse("2026-09-07T10:00:00Z"));
		when(roomRepository.findDashboardRooms(teacherId, MembershipStatus.ACTIVE, PageRequest.of(0, 5)))
				.thenReturn(List.of(room));

		TeacherDashboardResponse response = service.get(teacherId);

		assertThat(response).isEqualTo(new TeacherDashboardResponse(
				4, 3, 1, 2, 12, 8, 3, 17,
				List.of(new TeacherDashboardRoomResponse(
						roomId, "2º ano A", Grade.HIGH_SCHOOL_2, 18, false,
						Instant.parse("2026-09-07T10:00:00Z")
				))
		));
		assertThat(response.roomCount()).isEqualTo(response.activeRoomCount() + response.archivedRoomCount());
	}

	@Test
	void deveRejeitarProfessorInexistenteAntesDeConsultarContagens() {
		UUID teacherId = UUID.randomUUID();
		when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

		assertRejected(teacherId, HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND");
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"STUDENT", "ADMIN"})
	void deveRejeitarPapelIncompativelAntesDeConsultarContagens(Role role) {
		User user = user(role, AccountStatus.ACTIVE);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		assertRejected(user.getId(), HttpStatus.FORBIDDEN, "TEACHER_REQUIRED");
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = {"PENDING", "INACTIVE"})
	void deveRejeitarProfessorSemContaAtivaAntesDeConsultarContagens(AccountStatus status) {
		User teacher = user(Role.TEACHER, status);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

		assertRejected(teacher.getId(), HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
	}

	private void assertRejected(UUID userId, HttpStatus status, String code) {
		assertThatThrownBy(() -> service.get(userId))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(status);
					assertThat(exception.getCode()).isEqualTo(code);
				});
		verifyNoInteractions(roomRepository, membershipRepository, lessonRepository, attemptRepository);
	}

	private TeacherDashboardRoomProjection room(
			UUID id,
			String name,
			long studentCount,
			Instant archivedAt,
			Instant lastActivityAt
	) {
		TeacherDashboardRoomProjection room = mock(TeacherDashboardRoomProjection.class);
		when(room.getId()).thenReturn(id);
		when(room.getName()).thenReturn(name);
		when(room.getGrade()).thenReturn(Grade.HIGH_SCHOOL_2);
		when(room.getStudentCount()).thenReturn(studentCount);
		when(room.getArchivedAt()).thenReturn(archivedAt);
		when(room.getLastActivityAt()).thenReturn(lastActivityAt);
		return room;
	}

	private User user(Role role, AccountStatus status) {
		return new User(role, status, "Professor", "teacher@example.com", "REG-1", null);
	}
}
