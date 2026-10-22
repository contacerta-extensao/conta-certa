package com.ifsc.contacerta.mapper;

import com.ifsc.contacerta.dto.teacher.TeacherColleagueResponse;
import com.ifsc.contacerta.entity.User;

public final class TeacherColleagueMapper {

	private TeacherColleagueMapper() {
	}

	public static TeacherColleagueResponse toResponse(User teacher) {
		return new TeacherColleagueResponse(
				teacher.getId(),
				teacher.getFullName(),
				teacher.getEmail(),
				teacher.getRegistrationNumber(),
				teacher.getStatus(),
				teacher.getCreatedAt()
		);
	}
}
