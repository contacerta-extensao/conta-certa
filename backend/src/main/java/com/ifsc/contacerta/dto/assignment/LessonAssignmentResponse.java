package com.ifsc.contacerta.dto.assignment;

import com.ifsc.contacerta.model.ContentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Atribuição de lição a uma sala.
 *
 * {@code removable} diz se um {@code DELETE} nesta atribuição seria aceito:
 * atribuição já disponível para a turma precisa ser arquivada, não retirada.
 */
public record LessonAssignmentResponse(
		UUID id,
		UUID roomId,
		LessonReferenceResponse lesson,
		int position,
		ContentStatus status,
		Instant availableFrom,
		Instant dueAt,
		Integer timeLimitMinutes,
		Integer maxAttempts,
		Integer questionCount,
		boolean shuffleQuestions,
		boolean shuffleOptions,
		boolean removable,
		Instant createdAt,
		Instant updatedAt,
		long version
) {

	/** Lição referenciada, com as questões ativas que limitam {@code questionCount}. */
	public record LessonReferenceResponse(UUID id, String title, long activeQuestionCount) {
	}
}
