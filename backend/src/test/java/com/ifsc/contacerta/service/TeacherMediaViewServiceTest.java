package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.media.MediaViewsPageResponse;
import com.ifsc.contacerta.entity.Material;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.entity.Video;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.MediaViewType;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.MaterialRepository;
import com.ifsc.contacerta.repository.MediaViewRepository;
import com.ifsc.contacerta.repository.MediaViewerProjection;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherMediaViewServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

	@Mock private UserRepository userRepository;
	@Mock private VideoRepository videoRepository;
	@Mock private MaterialRepository materialRepository;
	@Mock private MediaViewRepository viewRepository;

	private TeacherMediaViewService service;
	private User teacher;

	@BeforeEach
	void setUp() {
		service = new TeacherMediaViewService(userRepository, videoRepository, materialRepository, viewRepository);
		teacher = new User(Role.TEACHER, AccountStatus.ACTIVE, "Professora", "prof@example.com", "P-1", null);
	}

	@Test
	void deveListarQuemAbriuOVideoUmaLinhaPorAluno() {
		UUID videoId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		Video video = new Video(teacher, "Juros", null, "Finanças", "https://example.com/v", NOW);
		MediaViewerProjection viewer = viewer(studentId, "Aluno Um", "S1");
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(videoRepository.findByIdAndTeacherId(videoId, teacher.getId())).thenReturn(Optional.of(video));
		when(viewRepository.findVideoViewers(videoId, teacher.getId(), PageRequest.of(0, 1)))
				.thenReturn(new PageImpl<>(List.of(viewer), PageRequest.of(0, 1), 3));

		MediaViewsPageResponse response = service.views(
				teacher.getId(), MediaViewType.VIDEO, videoId, 0, 1
		);

		assertThat(response.content()).singleElement().satisfies(view -> {
			assertThat(view.studentId()).isEqualTo(studentId);
			assertThat(view.fullName()).isEqualTo("Aluno Um");
			assertThat(view.registrationNumber()).isEqualTo("S1");
			assertThat(view.firstViewedAt()).isEqualTo(NOW.minusSeconds(600));
			assertThat(view.lastViewedAt()).isEqualTo(NOW);
		});
		assertThat(response.totalElements()).isEqualTo(3);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.totalViewers()).isEqualTo(3);
		verifyNoInteractions(materialRepository);
	}

	@Test
	void deveListarQuemAbriuOMaterial() {
		UUID materialId = UUID.randomUUID();
		Material material = Material.externalLink(teacher, "Apostila", null, "Porcentagem", "https://example.com/a", NOW);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(materialRepository.findByIdAndTeacherId(materialId, teacher.getId())).thenReturn(Optional.of(material));
		when(viewRepository.findMaterialViewers(materialId, teacher.getId(), PageRequest.of(1, 10)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

		MediaViewsPageResponse response = service.views(
				teacher.getId(), MediaViewType.MATERIAL, materialId, 1, 10
		);

		assertThat(response.content()).isEmpty();
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(10);
		verifyNoInteractions(videoRepository);
	}

	@Test
	void deveEsconderMidiaDeOutroProfessor() {
		UUID videoId = UUID.randomUUID();
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(videoRepository.findByIdAndTeacherId(videoId, teacher.getId())).thenReturn(Optional.empty());

		assertError(
				() -> service.views(teacher.getId(), MediaViewType.VIDEO, videoId, 0, 20),
				HttpStatus.NOT_FOUND,
				"VIDEO_NOT_FOUND"
		);
		verifyNoInteractions(viewRepository);
	}

	@Test
	void deveRejeitarPaginacaoForaDosLimites() {
		UUID videoId = UUID.randomUUID();
		Video video = new Video(teacher, "Juros", null, null, "https://example.com/v", NOW);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(videoRepository.findByIdAndTeacherId(videoId, teacher.getId())).thenReturn(Optional.of(video));

		assertError(
				() -> service.views(teacher.getId(), MediaViewType.VIDEO, videoId, 0, 101),
				HttpStatus.BAD_REQUEST,
				"BAD_REQUEST"
		);
		verifyNoInteractions(viewRepository);
	}

	@Test
	void deveRejeitarContaSemPapelDeProfessor() {
		User student = new User(Role.STUDENT, AccountStatus.ACTIVE, "Aluno", "aluno@example.com", "A-1", null);
		when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

		assertError(
				() -> service.views(student.getId(), MediaViewType.VIDEO, UUID.randomUUID(), 0, 20),
				HttpStatus.FORBIDDEN,
				"TEACHER_REQUIRED"
		);
		verifyNoInteractions(videoRepository, materialRepository, viewRepository);
	}

	private MediaViewerProjection viewer(UUID studentId, String fullName, String registrationNumber) {
		MediaViewerProjection viewer = mock(MediaViewerProjection.class);
		when(viewer.getStudentId()).thenReturn(studentId);
		when(viewer.getFullName()).thenReturn(fullName);
		when(viewer.getRegistrationNumber()).thenReturn(registrationNumber);
		when(viewer.getFirstViewedAt()).thenReturn(NOW.minusSeconds(600));
		when(viewer.getLastViewedAt()).thenReturn(NOW);
		return viewer;
	}

	private void assertError(Runnable action, HttpStatus status, String code) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(status);
					assertThat(exception.getCode()).isEqualTo(code);
				});
	}
}
