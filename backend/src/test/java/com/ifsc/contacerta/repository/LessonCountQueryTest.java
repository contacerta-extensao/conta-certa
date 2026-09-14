package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.LessonAssignment;
import com.ifsc.contacerta.entity.Question;
import com.ifsc.contacerta.entity.QuestionOptionData;
import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.QuestionType;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Contagens que alimentam as colunas "Questões" e "Salas" do acervo.
 *
 * Questão arquivada logicamente ({@code active = false}) e atribuição arquivada
 * não contam: a lição não está mais em uso naquela sala.
 */
@Transactional
class LessonCountQueryTest extends PostgresIntegrationTest {

	@Autowired private QuestionRepository questionRepository;
	@Autowired private LessonAssignmentRepository assignmentRepository;
	@Autowired private EntityManager entityManager;

	@Test
	void deveContarQuestoesAtivasESalasEmUsoPorLicao() {
		Institution institution = persist(new Institution(
				"Instituto", "11222333000181", "contato@example.com", "48999990000", true
		));
		User teacher = persist(new User(
				Role.TEACHER, AccountStatus.ACTIVE, "Professora", "prof@example.com", "P-1", institution
		));
		Lesson used = persist(new Lesson("Juros compostos", null, "# Teoria", teacher));
		Lesson unused = persist(new Lesson("Porcentagem", null, "# Teoria", teacher));
		persist(question(used, "Ativa 1", 1));
		persist(question(used, "Ativa 2", 2));
		Question archived = persist(question(used, "Arquivada", 3));
		archived.archive();
		Room firstRoom = persist(room("SAL001", teacher, institution));
		Room secondRoom = persist(room("SAL002", teacher, institution));
		Room thirdRoom = persist(room("SAL003", teacher, institution));
		persist(assignment(firstRoom, used, 1));
		persist(assignment(secondRoom, used, 1));
		LessonAssignment archivedAssignment = persist(assignment(thirdRoom, used, 1));
		archivedAssignment.archive();
		entityManager.flush();
		entityManager.clear();

		List<LessonCountProjection> questionCounts = questionRepository.countActiveByLessonIds(
				List.of(used.getId(), unused.getId())
		);
		List<LessonCountProjection> assignmentCounts = assignmentRepository.countByLessonIdsAndStatusNot(
				List.of(used.getId(), unused.getId()), ContentStatus.ARCHIVED
		);

		assertThat(questionCounts)
				.extracting(LessonCountProjection::getLessonId, LessonCountProjection::getTotal)
				.containsExactly(tuple(used.getId(), 2L));
		assertThat(assignmentCounts)
				.extracting(LessonCountProjection::getLessonId, LessonCountProjection::getTotal)
				.containsExactly(tuple(used.getId(), 2L));
		assertThat(questionRepository.countByLessonIdAndActiveTrue(used.getId())).isEqualTo(2);
		assertThat(assignmentRepository.countByLessonIdAndStatusNot(used.getId(), ContentStatus.ARCHIVED))
				.isEqualTo(2);
		assertThat(assignmentRepository.countByLessonIdAndStatusNot(unused.getId(), ContentStatus.ARCHIVED))
				.isZero();
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private Question question(Lesson lesson, String prompt, int position) {
		return Question.create(
				lesson,
				QuestionType.SINGLE_CHOICE,
				prompt,
				null,
				List.of(new QuestionOptionData("1%", true), new QuestionOptionData("10%", false)),
				position
		);
	}

	private Room room(String code, User teacher, Institution institution) {
		return new Room(
				code, null, Grade.HIGH_SCHOOL_1, List.of("Porcentagem"), 60,
				code, "hash-" + code, teacher, institution
		);
	}

	private LessonAssignment assignment(Room room, Lesson lesson, int position) {
		return new LessonAssignment(room, lesson, position, null, null, 30, 3, null, true, true);
	}
}
