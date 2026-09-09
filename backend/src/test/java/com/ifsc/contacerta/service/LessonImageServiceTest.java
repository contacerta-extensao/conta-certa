package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.lesson.LessonImageResponse;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.LessonImage;
import com.ifsc.contacerta.entity.StoredFile;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.mapper.FileDownloadMapper;
import com.ifsc.contacerta.mapper.LessonImageMapper;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.FileDownload;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.model.ValidatedUpload;
import com.ifsc.contacerta.repository.LessonImageRepository;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonImageServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
	private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	@Mock private UserRepository userRepository;
	@Mock private LessonRepository lessonRepository;
	@Mock private LessonImageRepository imageRepository;
	@Mock private LessonImageValidator validator;
	@Mock private FileStorage storage;
	@Mock private FileDownloadMapper downloadMapper;
	@Mock private LessonImageMapper imageMapper;

	private LessonImageService service;
	private User teacher;
	private Lesson lesson;

	@BeforeEach
	void setUp() {
		service = new LessonImageService(
				userRepository, lessonRepository, imageRepository, validator, storage,
				downloadMapper, imageMapper, Clock.fixed(NOW, ZoneOffset.UTC)
		);
		teacher = new User(Role.TEACHER, AccountStatus.ACTIVE, "Professora", "prof@example.com", "P-1", null);
		lesson = new Lesson("Juros compostos", null, "# Teoria", teacher);
	}

	@Test
	void deveGuardarImagemDaLicaoDoProprioProfessor() {
		MockMultipartFile file = new MockMultipartFile("file", "grafico.png", "image/png", PNG);
		ValidatedUpload upload = new ValidatedUpload("grafico.png", "image/png", PNG);
		StoredFile stored = new StoredFile(teacher, "grafico.png", "image/png", PNG.length, "sha", PNG, NOW);
		LessonImageResponse expected = new LessonImageResponse(
				UUID.randomUUID(), "/api/v1/lesson-images/abc", "grafico.png"
		);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(lessonRepository.findByIdAndTeacherId(lesson.getId(), teacher.getId())).thenReturn(Optional.of(lesson));
		when(validator.validate(file)).thenReturn(upload);
		when(storage.store(teacher, upload, NOW)).thenReturn(stored);
		when(imageRepository.save(any(LessonImage.class))).thenAnswer(call -> call.getArgument(0));
		when(imageMapper.toResponse(any(LessonImage.class))).thenReturn(expected);

		LessonImageResponse response = service.upload(teacher.getId(), lesson.getId(), file);

		assertThat(response).isSameAs(expected);
		ArgumentCaptor<LessonImage> saved = ArgumentCaptor.forClass(LessonImage.class);
		verify(imageRepository).save(saved.capture());
		assertThat(saved.getValue().getLesson()).isSameAs(lesson);
		assertThat(saved.getValue().getFile()).isSameAs(stored);
		assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void deveRejeitarLicaoDeOutroProfessorAntesDeLerOArquivo() {
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(lessonRepository.findByIdAndTeacherId(lesson.getId(), teacher.getId())).thenReturn(Optional.empty());

		assertError(
				() -> service.upload(teacher.getId(), lesson.getId(), null),
				HttpStatus.NOT_FOUND,
				"LESSON_NOT_FOUND"
		);
		verifyNoInteractions(validator, storage, imageRepository);
	}

	@Test
	void deveRejeitarUploadEmLicaoArquivada() {
		lesson.archive();
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(lessonRepository.findByIdAndTeacherId(lesson.getId(), teacher.getId())).thenReturn(Optional.of(lesson));

		assertError(
				() -> service.upload(teacher.getId(), lesson.getId(), null),
				HttpStatus.UNPROCESSABLE_CONTENT,
				"LESSON_ARCHIVED"
		);
		verifyNoInteractions(validator, storage, imageRepository);
	}

	@Test
	void deveRejeitarContaSemPapelDeProfessor() {
		User student = new User(Role.STUDENT, AccountStatus.ACTIVE, "Aluno", "aluno@example.com", "A-1", null);
		when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

		assertError(
				() -> service.upload(student.getId(), lesson.getId(), null),
				HttpStatus.FORBIDDEN,
				"TEACHER_REQUIRED"
		);
		verifyNoInteractions(lessonRepository, validator, storage, imageRepository);
	}

	@Test
	void deveServirConteudoDaImagemSemDonoEsemToken() {
		StoredFile stored = new StoredFile(teacher, "grafico.png", "image/png", PNG.length, "sha", PNG, NOW);
		LessonImage image = new LessonImage(lesson, stored, NOW);
		FileDownload expected = new FileDownload("grafico.png", "image/png", PNG.length, PNG);
		when(imageRepository.findWithFileById(image.getId())).thenReturn(Optional.of(image));
		when(downloadMapper.toDownload(stored)).thenReturn(expected);

		assertThat(service.content(image.getId())).isSameAs(expected);
		verifyNoInteractions(userRepository);
	}

	@Test
	void deveResponderNaoEncontradoParaImagemInexistente() {
		UUID imageId = UUID.randomUUID();
		when(imageRepository.findWithFileById(imageId)).thenReturn(Optional.empty());

		assertError(() -> service.content(imageId), HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND");
	}

	private void assertError(Runnable action, HttpStatus status, String code) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(status);
					assertThat(exception.getCode()).isEqualTo(code);
				});
	}
}
