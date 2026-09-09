package com.ifsc.contacerta.specification;

import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.InstitutionRepository;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LessonSpecificationTest extends PostgresIntegrationTest {

	@Autowired private InstitutionRepository institutionRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private LessonRepository lessonRepository;

	@Test
	void deveBuscarEmTituloEResumoSemDiferenciarMaiusculasDentroDoAcervoDoProfessor() {
		Institution institution = institutionRepository.save(institution());
		User teacher = userRepository.save(teacher("ana@example.com", institution));
		User anotherTeacher = userRepository.save(teacher("bia@example.com", institution));
		Lesson byTitle = lessonRepository.save(new Lesson("Juros compostos", null, "# Teoria", teacher));
		Lesson bySummary = lessonRepository.save(new Lesson("Aula 2", "Fala de JUROS na prática", "# Teoria", teacher));
		lessonRepository.save(new Lesson("Porcentagem", "Sem relação", "# Teoria", teacher));
		lessonRepository.save(new Lesson("Juros compostos", null, "# Teoria", anotherTeacher));

		var result = lessonRepository.findAll(
				LessonSpecification.ownedBy(teacher.getId(), "jUrOs", null),
				PageRequest.of(0, 10)
		);

		assertThat(result.getContent()).extracting(Lesson::getId)
				.containsExactlyInAnyOrder(byTitle.getId(), bySummary.getId());
	}

	@Test
	void deveIgnorarBuscaEmBrancoEFiltrarPorSituacaoQuandoSolicitado() {
		Institution institution = institutionRepository.save(institution());
		User teacher = userRepository.save(teacher("carla@example.com", institution));
		Lesson draft = lessonRepository.save(new Lesson("Rascunho", null, "# Teoria", teacher));
		Lesson published = lessonRepository.save(new Lesson("Publicada", null, "# Teoria", teacher));
		published.publish();
		lessonRepository.save(published);

		var all = lessonRepository.findAll(
				LessonSpecification.ownedBy(teacher.getId(), "   ", null),
				PageRequest.of(0, 10)
		);
		var drafts = lessonRepository.findAll(
				LessonSpecification.ownedBy(teacher.getId(), null, ContentStatus.DRAFT),
				PageRequest.of(0, 10)
		);

		assertThat(all).hasSize(2);
		assertThat(drafts).extracting(Lesson::getId).containsExactly(draft.getId());
	}

	private Institution institution() {
		return new Institution("Instituto Exemplo", "11222333000181", "contato@example.com", "48999990000", true);
	}

	private User teacher(String email, Institution institution) {
		return new User(Role.TEACHER, AccountStatus.ACTIVE, "Professora Ana", email, "PROF-1", institution);
	}
}
