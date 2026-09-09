package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.Material;
import com.ifsc.contacerta.entity.MediaView;
import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.entity.RoomMembership;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.entity.Video;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MediaViewRepositoryTest extends PostgresIntegrationTest {

	private static final Instant FIRST = Instant.parse("2026-09-01T10:00:00Z");
	private static final Instant LATER = Instant.parse("2026-09-05T18:00:00Z");

	@Autowired private MediaViewRepository viewRepository;
	@Autowired private EntityManager entityManager;

	@Test
	void deveAgregarVisualizacoesDoVideoPorAlunoNasSalasDoProfessor() {
		Institution institution = persist(new Institution(
				"Instituto", "11222333000181", "contato@example.com", "48999990000", true
		));
		User teacher = persist(user(Role.TEACHER, "Professora", "prof@example.com", "P-1", institution));
		User otherTeacher = persist(user(Role.TEACHER, "Outro", "outro@example.com", "P-2", institution));
		User student = persist(user(Role.STUDENT, "Ana Souza", "ana@example.com", "A-1", institution));
		User quietStudent = persist(user(Role.STUDENT, "Bruno Lima", "bruno@example.com", "A-2", institution));
		Room room = persist(room("SALA01", teacher, institution));
		Room otherRoom = persist(room("SALA02", teacher, institution));
		Room foreignRoom = persist(room("SALA03", otherTeacher, institution));
		persist(new RoomMembership(room, student));
		persist(new RoomMembership(otherRoom, student));
		persist(new RoomMembership(foreignRoom, quietStudent));
		Video video = persist(new Video(teacher, "Juros", null, "Finanças", "https://example.com/v", FIRST));
		persist(MediaView.video(student, room, video, FIRST));
		persist(MediaView.video(student, otherRoom, video, LATER));
		persist(MediaView.video(quietStudent, foreignRoom, video, LATER));
		entityManager.flush();
		entityManager.clear();

		Page<MediaViewerProjection> viewers = viewRepository.findVideoViewers(
				video.getId(), teacher.getId(), PageRequest.of(0, 20)
		);

		assertThat(viewers.getTotalElements()).isEqualTo(1);
		assertThat(viewers.getContent()).singleElement().satisfies(viewer -> {
			assertThat(viewer.getStudentId()).isEqualTo(student.getId());
			assertThat(viewer.getFullName()).isEqualTo("Ana Souza");
			assertThat(viewer.getRegistrationNumber()).isEqualTo("A-1");
			assertThat(viewer.getFirstViewedAt()).isEqualTo(FIRST);
			assertThat(viewer.getLastViewedAt()).isEqualTo(LATER);
		});
	}

	@Test
	void deveListarQuemAbriuOMaterialEmOrdemDeAberturaMaisRecente() {
		Institution institution = persist(new Institution(
				"Instituto", "11222333000181", "contato@example.com", "48999990000", true
		));
		User teacher = persist(user(Role.TEACHER, "Professora", "prof@example.com", "P-1", institution));
		User recent = persist(user(Role.STUDENT, "Ana Souza", "ana@example.com", "A-1", institution));
		User older = persist(user(Role.STUDENT, "Bruno Lima", "bruno@example.com", "A-2", institution));
		Room room = persist(room("SALA01", teacher, institution));
		persist(new RoomMembership(room, recent));
		persist(new RoomMembership(room, older));
		Material material = persist(Material.externalLink(
				teacher, "Apostila", null, "Porcentagem", "https://example.com/a", FIRST
		));
		persist(MediaView.material(older, room, material, FIRST));
		persist(MediaView.material(recent, room, material, LATER));
		entityManager.flush();
		entityManager.clear();

		Page<MediaViewerProjection> viewers = viewRepository.findMaterialViewers(
				material.getId(), teacher.getId(), PageRequest.of(0, 1)
		);

		assertThat(viewers.getTotalElements()).isEqualTo(2);
		assertThat(viewers.getTotalPages()).isEqualTo(2);
		assertThat(viewers.getContent())
				.extracting(MediaViewerProjection::getStudentId)
				.containsExactly(recent.getId());
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private User user(Role role, String name, String email, String registration, Institution institution) {
		return new User(role, AccountStatus.ACTIVE, name, email, registration, institution);
	}

	private Room room(String code, User teacher, Institution institution) {
		return new Room(
				code, null, Grade.HIGH_SCHOOL_1, List.of("Porcentagem"), 60,
				code, "hash-" + code, teacher, institution
		);
	}
}
