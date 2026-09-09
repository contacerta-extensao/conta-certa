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
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.FileDownload;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.LessonImageRepository;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonImageService {

	private final UserRepository userRepository;
	private final LessonRepository lessonRepository;
	private final LessonImageRepository imageRepository;
	private final LessonImageValidator validator;
	private final FileStorage storage;
	private final FileDownloadMapper downloadMapper;
	private final LessonImageMapper imageMapper;
	private final Clock clock;

	@Transactional
	public LessonImageResponse upload(UUID teacherId, UUID lessonId, MultipartFile file) {
		User teacher = requireActiveTeacher(teacherId);
		Lesson lesson = lessonRepository.findByIdAndTeacherId(lessonId, teacherId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Lesson was not found."
		));
		if (lesson.getStatus() == ContentStatus.ARCHIVED) {
			throw new ApiException(
					HttpStatus.UNPROCESSABLE_CONTENT, "LESSON_ARCHIVED", "Archived lessons are read-only."
			);
		}
		StoredFile stored = storage.store(teacher, validator.validate(file), Instant.now(clock));
		return imageMapper.toResponse(imageRepository.save(new LessonImage(lesson, stored, Instant.now(clock))));
	}

	/**
	 * Conteúdo da imagem, sem autenticação.
	 *
	 * O {@code <img>} do Markdown não manda cabeçalho de autorização, então a
	 * imagem é lida por identificador aleatório. Só arquivo registrado como
	 * imagem de lição passa por aqui — material em PDF continua exigindo token
	 * em {@code /files/{fileId}/download}.
	 */
	@Transactional(readOnly = true)
	public FileDownload content(UUID imageId) {
		LessonImage image = imageRepository.findWithFileById(imageId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Image was not found."
		));
		return downloadMapper.toDownload(image.getFile());
	}

	private User requireActiveTeacher(UUID teacherId) {
		User teacher = userRepository.findById(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND", "Teacher was not found."));
		if (teacher.getRole() != Role.TEACHER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "TEACHER_REQUIRED", "A teacher account is required.");
		}
		if (teacher.getStatus() != AccountStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Teacher account is inactive.");
		}
		return teacher;
	}
}
