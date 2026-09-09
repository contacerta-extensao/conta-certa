package com.ifsc.contacerta.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Conclusão por lição.
 *
 * {@code completedStudents} conta alunos aprovados na lição dentro do recorte e
 * {@code totalStudents}, as matrículas ativas da sala.
 */
public record ReportLessonCompletionResponse(
		UUID lessonId,
		String lessonTitle,
		long completedStudents,
		long totalStudents,
		BigDecimal completionPercent,
		BigDecimal averageScorePercent,
		long participatingStudentCount,
		long attemptCount,
		BigDecimal passRatePercent
) { }
