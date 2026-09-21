package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.LessonAssignment;
import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.entity.RoomMembership;
import com.ifsc.contacerta.entity.RoomStudentProgress;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.MembershipStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.service.JoinCodeHasher;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RoomMembershipRepositoryTest extends PostgresIntegrationTest {

	@Autowired private EntityManager entityManager;
	@Autowired private InstitutionRepository institutionRepository;
	@Autowired private LessonRepository lessonRepository;
	@Autowired private LessonAssignmentRepository assignmentRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private RoomRepository roomRepository;
	@Autowired private RoomMembershipRepository membershipRepository;
	@Autowired private RoomStudentProgressRepository progressRepository;

	@Test
	void deveProjetarMatriculasAtivasEmOrdemDecrescenteDeIngresso() {
		Institution institution = institutionRepository.save(new Institution(
				"Instituto Exemplo", "11222333000181", "contato@example.com", "48999990000", true
		));
		User teacher = userRepository.save(user(Role.TEACHER, "Professora Ana", "ana@example.com", institution));
		User olderStudent = userRepository.save(user(Role.STUDENT, "Aluno Bruno", "bruno@example.com", institution));
		User newerStudent = userRepository.save(user(Role.STUDENT, "Aluna Carla", "carla@example.com", institution));
		Room room = roomRepository.save(new Room(
				"1º ano A", null, Grade.HIGH_SCHOOL_1, List.of("Porcentagem"), 50,
				"ABC123", new JoinCodeHasher().hash("ABC123"), teacher, institution
		));
		RoomMembership olderMembership = membershipRepository.save(new RoomMembership(room, olderStudent));
		RoomMembership newerMembership = membershipRepository.save(new RoomMembership(room, newerStudent));
		entityManager.flush();
		entityManager.createNativeQuery("update room_memberships set joined_at = :joinedAt where id = :membershipId")
				.setParameter("joinedAt", Instant.parse("2026-01-01T00:00:00Z"))
				.setParameter("membershipId", olderMembership.getId())
				.executeUpdate();
		entityManager.createNativeQuery("update room_memberships set joined_at = :joinedAt where id = :membershipId")
				.setParameter("joinedAt", Instant.parse("2026-02-01T00:00:00Z"))
				.setParameter("membershipId", newerMembership.getId())
				.executeUpdate();
		entityManager.clear();

		var result = membershipRepository.findStudentResponsesByRoomIdAndStatusOrderByJoinedAtDesc(
				room.getId(), MembershipStatus.ACTIVE, PageRequest.of(0, 10)
		);

		assertThat(result.getContent())
				.extracting(response -> response.studentId())
				.containsExactly(newerStudent.getId(), olderStudent.getId());
		assertThat(result.getContent())
				.allSatisfy(response -> {
					assertThat(response.xp()).isZero();
					assertThat(response.completedLessons()).isZero();
					assertThat(response.totalLessons()).isZero();
					assertThat(response.stars()).isZero();
					assertThat(response.lastActivityAt()).isNull();
					assertThat(response.membershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
				});
	}

	@Test
	void deveProjetarMetricasDeExperienciaDaSala() {
		Institution institution = institutionRepository.save(new Institution(
				"Instituto Métricas", "11222333000181", "metricas@example.com", "48999990000", true
		));
		User teacher = userRepository.save(user(Role.TEACHER, "Professora Métricas", "metricas-teacher@example.com", institution));
		User student = userRepository.save(user(Role.STUDENT, "Aluno Métricas", "metricas-student@example.com", institution));
		Room room = roomRepository.save(new Room(
				"Sala Métricas", null, Grade.HIGH_SCHOOL_1, List.of("Porcentagem"), 50,
				"MET123", new JoinCodeHasher().hash("MET123"), teacher, institution
		));
		membershipRepository.save(new RoomMembership(room, student));
		Lesson lesson = lessonRepository.save(new Lesson("Lição publicada", null, "Teoria", teacher));
		lesson.publish();
		lessonRepository.saveAndFlush(lesson);
		LessonAssignment assignment = new LessonAssignment(
				room, lesson, 1, null, null, 30, 3, null, true, true
		);
		assignment.publish();
		assignmentRepository.saveAndFlush(assignment);
		RoomStudentProgress progress = progressRepository.save(new RoomStudentProgress(room, student));
		Instant activityAt = Instant.parse("2026-09-21T12:00:00Z");
		progress.applyResult(120, 3, true, true, activityAt);
		progressRepository.saveAndFlush(progress);
		entityManager.clear();

		var response = membershipRepository.findStudentResponsesByRoomIdAndStatusOrderByJoinedAtDesc(
				room.getId(), MembershipStatus.ACTIVE, PageRequest.of(0, 10)
		).getContent().getFirst();

		assertThat(response.xp()).isEqualTo(120);
		assertThat(response.level()).isEqualTo(2);
		assertThat(response.completedLessons()).isEqualTo(1);
		assertThat(response.totalLessons()).isEqualTo(1);
		assertThat(response.stars()).isEqualTo(3);
		assertThat(response.lastActivityAt()).isEqualTo(activityAt);
	}

	private User user(Role role, String name, String email, Institution institution) {
		return new User(role, AccountStatus.ACTIVE, name, email, role == Role.TEACHER ? "PROF-1" : "ALUNO-1", institution);
	}
}
