package com.ifsc.contacerta.dto.teacher;

import com.ifsc.contacerta.model.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record TeacherColleagueResponse(
		UUID id,
		String fullName,
		String email,
		String registrationNumber,
		AccountStatus status,
		Instant createdAt
) {
}
