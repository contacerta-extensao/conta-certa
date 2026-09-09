package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.lesson.CreateLessonRequest;
import com.ifsc.contacerta.dto.lesson.LessonSummaryResponse;
import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.LessonAssignmentRepository;
import com.ifsc.contacerta.repository.LessonCountProjection;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.QuestionRepository;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LessonServiceTest {

	private UserRepository userRepository;
	private LessonRepository lessonRepository;
	private QuestionRepository questionRepository;
	private LessonAssignmentRepository assignmentRepository;
	private LessonService service;
	private User teacher;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		lessonRepository = mock(LessonRepository.class);
		questionRepository = mock(QuestionRepository.class);
		assignmentRepository = mock(LessonAssignmentRepository.class);
		service = new LessonService(userRepository, lessonRepository, questionRepository, assignmentRepository);
		Institution institution = new Institution(
				"Instituto Exemplo", "11222333000181", "contato@example.com", "48999990000", true
		);
		teacher = new User(
				Role.TEACHER, AccountStatus.ACTIVE, "Professora Ana", "ana@example.com", "PROF-1", institution
		);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
	}

	@Test
	void deveCriarLicaoRascunhoNoAcervoDoProfessorAtivo() {
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(teacher.getId(), new CreateLessonRequest(
				"Juros compostos", "Conceitos", "# Teoria"
		));

		assertThat(response.title()).isEqualTo("Juros compostos");
		assertThat(response.status()).isEqualTo(ContentStatus.DRAFT);
		assertThat(response.questionCount()).isZero();
		assertThat(response.assignmentCount()).isZero();
	}

	@Test
	void deveContarQuestoesAtivasESalasQueUsamCadaLicaoDaPagina() {
		Lesson withQuestions = new Lesson("Juros compostos", null, "# Teoria", teacher);
		Lesson untouched = new Lesson("Porcentagem", null, "# Teoria", teacher);
		PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title"));
		List<LessonCountProjection> questionCounts = List.of(count(withQuestions.getId(), 7));
		List<LessonCountProjection> assignmentCounts = List.of(count(withQuestions.getId(), 2));
		when(lessonRepository.findAll(any(Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(withQuestions, untouched), pageable, 2));
		when(questionRepository.countActiveByLessonIds(anyList())).thenReturn(questionCounts);
		when(assignmentRepository.countByLessonIdsAndStatusNot(anyList(), eq(ContentStatus.ARCHIVED)))
				.thenReturn(assignmentCounts);

		var page = service.list(teacher.getId(), null, null, pageable);

		assertThat(page.content())
				.extracting(
						LessonSummaryResponse::title,
						LessonSummaryResponse::questionCount,
						LessonSummaryResponse::assignmentCount
				)
				.containsExactly(
						tuple("Juros compostos", 7L, 2L),
						tuple("Porcentagem", 0L, 0L)
				);
	}

	@Test
	void naoDeveContarNadaQuandoAPaginaVemVazia() {
		PageRequest pageable = PageRequest.of(3, 20);
		when(lessonRepository.findAll(any(Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		assertThat(service.list(teacher.getId(), null, null, pageable).content()).isEmpty();
		verifyNoInteractions(questionRepository, assignmentRepository);
	}

	@Test
	void deveExporContagensNoDetalheDaLicao() {
		Lesson lesson = new Lesson("Juros compostos", null, "# Teoria", teacher);
		when(lessonRepository.findByIdAndTeacherId(lesson.getId(), teacher.getId())).thenReturn(Optional.of(lesson));
		when(questionRepository.countByLessonIdAndActiveTrue(lesson.getId())).thenReturn(4L);
		when(assignmentRepository.countByLessonIdAndStatusNot(lesson.getId(), ContentStatus.ARCHIVED)).thenReturn(3L);

		var response = service.get(teacher.getId(), lesson.getId());

		assertThat(response.questionCount()).isEqualTo(4);
		assertThat(response.assignmentCount()).isEqualTo(3);
	}

	@Test
	void deveImpedirPublicacaoDeLicaoSemQuestaoAtiva() {
		Lesson lesson = new Lesson("Juros compostos", "Conceitos", "# Teoria", teacher);
		when(lessonRepository.findByIdAndTeacherId(lesson.getId(), teacher.getId())).thenReturn(Optional.of(lesson));

		assertThatThrownBy(() -> service.publish(teacher.getId(), lesson.getId()))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus().value()).isEqualTo(422);
					assertThat(exception.getCode()).isEqualTo("LESSON_HAS_NO_ACTIVE_QUESTIONS");
				});
	}

	private LessonCountProjection count(UUID lessonId, long total) {
		LessonCountProjection projection = mock(LessonCountProjection.class);
		when(projection.getLessonId()).thenReturn(lessonId);
		when(projection.getTotal()).thenReturn(total);
		return projection;
	}
}
