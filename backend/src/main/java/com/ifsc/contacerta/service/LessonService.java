package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.lesson.CreateLessonRequest;
import com.ifsc.contacerta.dto.lesson.LessonDetailResponse;
import com.ifsc.contacerta.dto.lesson.LessonSummaryResponse;
import com.ifsc.contacerta.dto.lesson.UpdateLessonRequest;
import com.ifsc.contacerta.dto.shared.PageResponse;
import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.Question;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.LessonAssignmentRepository;
import com.ifsc.contacerta.repository.LessonCountProjection;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.QuestionRepository;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.specification.LessonSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

	private final UserRepository userRepository;
	private final LessonRepository lessonRepository;
	private final QuestionRepository questionRepository;
	private final LessonAssignmentRepository assignmentRepository;

	@Transactional
	public LessonDetailResponse create(UUID teacherId, CreateLessonRequest request) {
		User teacher = requireActiveTeacher(teacherId);
		Lesson lesson = lessonRepository.save(new Lesson(
				request.title(), request.summary(), request.theoryMarkdown(), teacher
		));
		return toDetailResponse(lesson);
	}

	@Transactional
	public LessonDetailResponse publish(UUID teacherId, UUID lessonId) {
		requireActiveTeacher(teacherId);
		Lesson lesson = lessonRepository.findByIdAndTeacherId(lessonId, teacherId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Lesson was not found."
		));
		if (questionRepository.countByLessonIdAndActiveTrue(lessonId) == 0) {
			throw new ApiException(
					HttpStatus.UNPROCESSABLE_CONTENT,
					"LESSON_HAS_NO_ACTIVE_QUESTIONS",
					"A lesson needs at least one active question to be published."
			);
		}
		lesson.publish();
		return toDetailResponse(lesson);
	}

	@Transactional(readOnly = true)
	public PageResponse<LessonSummaryResponse> list(
			UUID teacherId,
			String search,
			ContentStatus status,
			Pageable pageable
	) {
		requireActiveTeacher(teacherId);
		Page<Lesson> lessons = lessonRepository
				.findAll(LessonSpecification.ownedBy(teacherId, search, status), pageable);
		List<UUID> lessonIds = lessons.getContent().stream().map(Lesson::getId).toList();
		Map<UUID, Long> questionCounts = countsByLesson(
				lessonIds.isEmpty() ? List.of() : questionRepository.countActiveByLessonIds(lessonIds)
		);
		Map<UUID, Long> assignmentCounts = countsByLesson(
				lessonIds.isEmpty()
						? List.of()
						: assignmentRepository.countByLessonIdsAndStatusNot(lessonIds, ContentStatus.ARCHIVED)
		);
		return PageResponse.from(lessons.map(lesson -> toSummaryResponse(
				lesson,
				questionCounts.getOrDefault(lesson.getId(), 0L),
				assignmentCounts.getOrDefault(lesson.getId(), 0L)
		)));
	}

	@Transactional(readOnly = true)
	public LessonDetailResponse get(UUID teacherId, UUID lessonId) {
		requireActiveTeacher(teacherId);
		return toDetailResponse(requireOwnedLesson(teacherId, lessonId));
	}

	@Transactional
	public LessonDetailResponse update(UUID teacherId, UUID lessonId, UpdateLessonRequest request) {
		requireActiveTeacher(teacherId);
		Lesson lesson = requireOwnedLesson(teacherId, lessonId);
		requireVersion(lesson, request.version());
		if (lesson.getStatus() == ContentStatus.ARCHIVED) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "LESSON_ARCHIVED", "Archived lessons are read-only.");
		}
		lesson.update(request.title() == null ? lesson.getTitle() : request.title(), request.summary(), request.theoryMarkdown() == null ? lesson.getTheoryMarkdown() : request.theoryMarkdown());
		return toDetailResponse(lesson);
	}

	@Transactional
	public LessonDetailResponse archive(UUID teacherId, UUID lessonId) {
		requireActiveTeacher(teacherId);
		Lesson lesson = requireOwnedLesson(teacherId, lessonId);
		lesson.archive();
		return toDetailResponse(lesson);
	}

	@Transactional
	public LessonDetailResponse duplicate(UUID teacherId, UUID lessonId) {
		requireActiveTeacher(teacherId);
		Lesson source = requireOwnedLesson(teacherId, lessonId);
		Lesson copy = lessonRepository.save(source.duplicate(source.getTitle() + " (cópia)"));
		List<Question> questions = questionRepository.findByLessonIdAndActiveTrueOrderByPositionAsc(source.getId());
		for (int index = 0; index < questions.size(); index++) {
			questionRepository.save(questions.get(index).duplicateInto(copy, index + 1));
		}
		return toDetailResponse(copy);
	}

	private User requireActiveTeacher(UUID teacherId) {
		User teacher = userRepository.findById(teacherId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND", "Teacher was not found."
		));
		if (teacher.getRole() != Role.TEACHER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "TEACHER_REQUIRED", "A teacher account is required.");
		}
		if (teacher.getStatus() != AccountStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Teacher account is inactive.");
		}
		return teacher;
	}

	private Lesson requireOwnedLesson(UUID teacherId, UUID lessonId) {
		return lessonRepository.findByIdAndTeacherId(lessonId, teacherId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "LESSON_NOT_FOUND", "Lesson was not found."
		));
	}

	private void requireVersion(Lesson lesson, Long version) {
		if (version == null || version != lesson.getVersion()) {
			throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "The lesson was changed by another request.");
		}
	}

	/** Uma consulta agregada por página, em vez de duas contagens por lição. */
	private Map<UUID, Long> countsByLesson(List<LessonCountProjection> counts) {
		return counts.stream().collect(Collectors.toMap(
				LessonCountProjection::getLessonId, LessonCountProjection::getTotal
		));
	}

	private LessonSummaryResponse toSummaryResponse(
			Lesson lesson,
			long questionCount,
			long assignmentCount
	) {
		return new LessonSummaryResponse(
				lesson.getId(),
				lesson.getTitle(),
				lesson.getSummary(),
				lesson.getStatus(),
				questionCount,
				assignmentCount,
				lesson.getCreatedAt(),
				lesson.getUpdatedAt(),
				lesson.getVersion()
		);
	}

	private LessonDetailResponse toDetailResponse(Lesson lesson) {
		return new LessonDetailResponse(
				lesson.getId(),
				lesson.getTitle(),
				lesson.getSummary(),
				lesson.getTheoryMarkdown(),
				lesson.getStatus(),
				questionRepository.countByLessonIdAndActiveTrue(lesson.getId()),
				assignmentRepository.countByLessonIdAndStatusNot(lesson.getId(), ContentStatus.ARCHIVED),
				lesson.getCreatedAt(),
				lesson.getUpdatedAt(),
				lesson.getVersion()
		);
	}
}
