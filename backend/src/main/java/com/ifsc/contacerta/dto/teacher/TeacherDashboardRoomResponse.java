package com.ifsc.contacerta.dto.teacher;

import com.ifsc.contacerta.model.Grade;

import java.time.Instant;
import java.util.UUID;

public record TeacherDashboardRoomResponse(
		UUID id,
		String name,
		Grade grade,
		long studentCount,
		boolean archived,
		Instant lastActivityAt
) {
}
