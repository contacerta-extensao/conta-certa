package com.ifsc.contacerta.dto.studentdashboard;

import java.util.UUID;

public record StudentNextLessonResponse(
		UUID assignmentId,
		UUID lessonId,
		String title,
		int order,
		UUID activeAttemptId
) {
}
