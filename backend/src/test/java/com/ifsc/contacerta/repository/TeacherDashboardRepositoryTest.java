package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.Attempt;
import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.LessonAssignment;
import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.entity.RoomMembership;
import com.ifsc.contacerta.entity.RoomStudentProgress;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AttemptStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.MembershipStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TeacherDashboardRepositoryTest extends PostgresIntegrationTest {

	@Autowired private EntityManager entityManager;
	@Autowired private RoomRepository roomRepository;
	@Autowired private RoomMembershipRepository membershipRepository;
	@Autowired private LessonRepository lessonRepository;
	@Autowired private LessonAssignmentRepository assignmentRepository;
	@Autowired private AttemptRepository attemptRepository;

	@Test
	void deveRetornarZeroParaProfessorSemDados() {
		UUID teacherId = persistUser("sem-dados", Role.TEACHER, persistInstitution()).getId();

		assertThat(roomRepository.countByTeacherId(teacherId)).isZero();
		assertThat(roomRepository.countByTeacherIdAndArchivedAtIsNull(teacherId)).isZero();
		assertThat(roomRepository.countByTeacherIdAndArchivedAtIsNotNull(teacherId)).isZero();
		assertThat(membershipRepository.countDistinctStudentsByTeacherId(teacherId)).isZero();
		for (MembershipStatus status : MembershipStatus.values()) {
			assertThat(membershipRepository.countByRoomTeacherIdAndStatus(teacherId, status)).isZero();
		}
		assertThat(lessonRepository.countByTeacherId(teacherId)).isZero();
		assertThat(assignmentRepository.countByRoomTeacherId(teacherId)).isZero();
		for (ContentStatus status : ContentStatus.values()) {
			assertThat(lessonRepository.countByTeacherIdAndStatus(teacherId, status)).isZero();
			assertThat(assignmentRepository.countByRoomTeacherIdAndStatus(teacherId, status)).isZero();
		}
	}

	@Test
	void deveIsolarContagensPorProfessorIncluindoArquivadosEMatriculasRemovidas() {
		Institution institution = persistInstitution();
		User teacher = persistUser("professor", Role.TEACHER, institution);
		User otherTeacher = persistUser("outro-professor", Role.TEACHER, institution);
		User student = persistUser("aluno", Role.STUDENT, institution);
		User removedStudent = persistUser("aluno-removido", Role.STUDENT, institution);
		User otherStudent = persistUser("outro-aluno", Role.STUDENT, institution);
		Room activeRoom = persistRoom("ABC234", teacher, institution);
		Room archivedRoom = persistRoom("DEF567", teacher, institution);
		archivedRoom.archive();
		Room otherRoom = persistRoom("GHI890", otherTeacher, institution);
		entityManager.persist(new RoomMembership(activeRoom, student));
		entityManager.persist(new RoomMembership(archivedRoom, student));
		RoomMembership removedMembership = new RoomMembership(activeRoom, removedStudent);
		removedMembership.remove(teacher);
		entityManager.persist(removedMembership);
		entityManager.persist(new RoomMembership(otherRoom, otherStudent));
		Lesson draft = persistLesson("Rascunho", teacher);
		Lesson published = persistLesson("Publicada", teacher);
		published.publish();
		Lesson archived = persistLesson("Arquivada", teacher);
		archived.archive();
		Lesson otherLesson = persistLesson("Outra", otherTeacher);
		otherLesson.publish();
		persistAssignment(activeRoom, draft, 1);
		persistAssignment(activeRoom, published, 2).publish();
		persistAssignment(archivedRoom, published, 1).publish();
		persistAssignment(archivedRoom, archived, 2).archive();
		persistAssignment(otherRoom, otherLesson, 1).publish();
		entityManager.flush();
		entityManager.clear();

		assertThat(roomRepository.countByTeacherId(teacher.getId())).isEqualTo(2);
		assertThat(roomRepository.countByTeacherIdAndArchivedAtIsNull(teacher.getId())).isEqualTo(1);
		assertThat(roomRepository.countByTeacherIdAndArchivedAtIsNotNull(teacher.getId())).isEqualTo(1);
		assertThat(roomRepository.countByTeacherId(otherTeacher.getId())).isEqualTo(1);
		assertThat(roomRepository.countByTeacherIdAndArchivedAtIsNull(otherTeacher.getId())).isEqualTo(1);
		assertThat(roomRepository.countByTeacherIdAndArchivedAtIsNotNull(otherTeacher.getId())).isZero();
		assertThat(membershipRepository.countDistinctStudentsByTeacherId(teacher.getId())).isEqualTo(2);
		assertThat(membershipRepository.countByRoomTeacherIdAndStatus(teacher.getId(), MembershipStatus.ACTIVE))
				.isEqualTo(2);
		assertThat(membershipRepository.countByRoomTeacherIdAndStatus(teacher.getId(), MembershipStatus.REMOVED))
				.isEqualTo(1);
		assertThat(membershipRepository.countDistinctStudentsByTeacherId(otherTeacher.getId())).isEqualTo(1);
		assertThat(membershipRepository.countByRoomTeacherIdAndStatus(otherTeacher.getId(), MembershipStatus.ACTIVE))
				.isEqualTo(1);
		assertThat(membershipRepository.countByRoomTeacherIdAndStatus(otherTeacher.getId(), MembershipStatus.REMOVED))
				.isZero();
		assertThat(lessonRepository.countByTeacherId(teacher.getId())).isEqualTo(3);
		assertThat(lessonRepository.countByTeacherId(otherTeacher.getId())).isEqualTo(1);
		assertThat(assignmentRepository.countByRoomTeacherId(teacher.getId())).isEqualTo(4);
		assertThat(assignmentRepository.countByRoomTeacherId(otherTeacher.getId())).isEqualTo(1);
		for (ContentStatus status : ContentStatus.values()) {
			assertThat(lessonRepository.countByTeacherIdAndStatus(teacher.getId(), status)).isEqualTo(1);
			assertThat(lessonRepository.countByTeacherIdAndStatus(otherTeacher.getId(), status))
					.isEqualTo(status == ContentStatus.PUBLISHED ? 1 : 0);
			assertThat(assignmentRepository.countByRoomTeacherIdAndStatus(teacher.getId(), status))
					.isEqualTo(status == ContentStatus.PUBLISHED ? 2 : 1);
			assertThat(assignmentRepository.countByRoomTeacherIdAndStatus(otherTeacher.getId(), status))
					.isEqualTo(status == ContentStatus.PUBLISHED ? 1 : 0);
		}
	}

	@Test
	void deveListarSalasDoPainelComAlunosAtivosEUltimaAtividade() {
		Institution institution = persistInstitution();
		User teacher = persistUser("professor-painel", Role.TEACHER, institution);
		User otherTeacher = persistUser("outro-professor-painel", Role.TEACHER, institution);
		User student = persistUser("aluno-painel", Role.STUDENT, institution);
		User removedStudent = persistUser("aluno-painel-removido", Role.STUDENT, institution);
		Room room = persistRoom("PNL001", teacher, institution);
		Room archivedRoom = persistRoom("PNL002", teacher, institution);
		archivedRoom.archive();
		Room otherRoom = persistRoom("PNL003", otherTeacher, institution);
		entityManager.persist(new RoomMembership(room, student));
		RoomMembership removedMembership = new RoomMembership(room, removedStudent);
		removedMembership.remove(teacher);
		entityManager.persist(removedMembership);
		entityManager.persist(new RoomMembership(otherRoom, student));
		RoomStudentProgress progress = new RoomStudentProgress(room, student);
		progress.applyResult(30, 3, true, true, Instant.parse("2026-09-07T10:00:00Z"));
		entityManager.persist(progress);
		entityManager.flush();
		entityManager.clear();

		List<TeacherDashboardRoomProjection> rooms = roomRepository.findDashboardRooms(
				teacher.getId(), MembershipStatus.ACTIVE, PageRequest.of(0, 5)
		);

		assertThat(rooms).extracting(TeacherDashboardRoomProjection::getId)
				.containsExactlyInAnyOrder(room.getId(), archivedRoom.getId());
		assertThat(rooms).filteredOn(candidate -> candidate.getId().equals(room.getId()))
				.singleElement().satisfies(card -> {
					assertThat(card.getName()).isEqualTo("PNL001");
					assertThat(card.getGrade()).isEqualTo(Grade.HIGH_SCHOOL_1);
					assertThat(card.getStudentCount()).isEqualTo(1);
					assertThat(card.getArchivedAt()).isNull();
					assertThat(card.getLastActivityAt()).isEqualTo(Instant.parse("2026-09-07T10:00:00Z"));
				});
		assertThat(rooms).filteredOn(candidate -> candidate.getId().equals(archivedRoom.getId()))
				.singleElement().satisfies(card -> {
					assertThat(card.getStudentCount()).isZero();
					assertThat(card.getArchivedAt()).isNotNull();
					assertThat(card.getLastActivityAt()).isNull();
				});
		assertThat(roomRepository.findDashboardRooms(
				teacher.getId(), MembershipStatus.ACTIVE, PageRequest.of(0, 1)
		)).hasSize(1);
	}

	@Test
	void deveContarSomenteTentativasFinalizadasDaJanelaEDoProprioProfessor() {
		Institution institution = persistInstitution();
		User teacher = persistUser("professor-tentativas", Role.TEACHER, institution);
		User otherTeacher = persistUser("outro-professor-tentativas", Role.TEACHER, institution);
		User student = persistUser("aluno-tentativas", Role.STUDENT, institution);
		Lesson lesson = persistLesson("Juros", teacher);
		lesson.publish();
		Lesson otherLesson = persistLesson("Outra lição", otherTeacher);
		otherLesson.publish();
		LessonAssignment assignment = persistAssignment(persistRoom("TNT001", teacher, institution), lesson, 1);
		LessonAssignment otherAssignment = persistAssignment(
				persistRoom("TNT002", otherTeacher, institution), otherLesson, 1
		);
		persistAttempt(assignment, student, 1, AttemptStatus.SUBMITTED, Instant.parse("2026-09-05T10:00:00Z"));
		persistAttempt(assignment, student, 2, AttemptStatus.EXPIRED, Instant.parse("2026-09-06T10:00:00Z"));
		persistAttempt(assignment, student, 3, AttemptStatus.SUBMITTED, Instant.parse("2026-08-20T10:00:00Z"));
		entityManager.persist(new Attempt(assignment, student, 4, Instant.parse("2026-09-07T10:00:00Z"), null));
		persistAttempt(otherAssignment, student, 1, AttemptStatus.SUBMITTED, Instant.parse("2026-09-05T10:00:00Z"));
		entityManager.flush();
		entityManager.clear();

		long recent = attemptRepository.countFinalizedByTeacherIdSince(
				teacher.getId(),
				List.of(AttemptStatus.SUBMITTED, AttemptStatus.EXPIRED),
				Instant.parse("2026-09-01T00:00:00Z")
		);

		assertThat(recent).isEqualTo(2);
	}

	private Attempt persistAttempt(
			LessonAssignment assignment,
			User student,
			int sequence,
			AttemptStatus status,
			Instant submittedAt
	) {
		Attempt attempt = new Attempt(assignment, student, sequence, submittedAt.minusSeconds(600), null);
		attempt.finalizeAs(status, submittedAt, 10, 10, 8, true, 3, 30);
		entityManager.persist(attempt);
		return attempt;
	}

	private Institution persistInstitution() {
		Institution institution = new Institution(
				"Instituto Exemplo", "11222333000181", "contato@example.com", "48999990000", true
		);
		entityManager.persist(institution);
		return institution;
	}

	private User persistUser(String name, Role role, Institution institution) {
		User user = new User(role, AccountStatus.ACTIVE, name, name + "@example.com", name, institution);
		entityManager.persist(user);
		return user;
	}

	private Room persistRoom(String code, User teacher, Institution institution) {
		Room room = new Room(code, null, Grade.HIGH_SCHOOL_1, List.of("Porcentagem"), 50,
				code, "hash-" + code, teacher, institution);
		entityManager.persist(room);
		return room;
	}

	private Lesson persistLesson(String title, User teacher) {
		Lesson lesson = new Lesson(title, null, "# Teoria", teacher);
		entityManager.persist(lesson);
		return lesson;
	}

	private LessonAssignment persistAssignment(Room room, Lesson lesson, int position) {
		LessonAssignment assignment = new LessonAssignment(
				room, lesson, position, null, null, 30, 3, null, true, true
		);
		entityManager.persist(assignment);
		return assignment;
	}
}
