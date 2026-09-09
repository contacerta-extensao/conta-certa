package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.model.ContentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID>, JpaSpecificationExecutor<Lesson> {

	long countByTeacherId(UUID teacherId);

	long countByTeacherIdAndStatus(UUID teacherId, ContentStatus status);

	Optional<Lesson> findByIdAndTeacherId(UUID id, UUID teacherId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select lesson from Lesson lesson where lesson.id = :lessonId and lesson.teacher.id = :teacherId")
	Optional<Lesson> findByIdAndTeacherIdForUpdate(
			@Param("lessonId") UUID lessonId,
			@Param("teacherId") UUID teacherId
	);
}
