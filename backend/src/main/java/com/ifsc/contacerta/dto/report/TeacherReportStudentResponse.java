package com.ifsc.contacerta.dto.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Linha por aluno do relatório.
 *
 * {@code completedLessons} conta lições aprovadas e {@code attemptedLessons},
 * lições com pelo menos uma tentativa finalizada. As notas são nulas quando o
 * aluno não tem tentativa no recorte.
 */
public record TeacherReportStudentResponse(
		UUID studentId,
		String fullName,
		String registrationNumber,
		String email,
		int xp,
		int stars,
		int level,
		int completedLessons,
		int attemptedLessons,
		long totalLessons,
		long attemptCount,
		BigDecimal averageScorePercent,
		BigDecimal bestScorePercent,
		Instant lastActivityAt
) { }
