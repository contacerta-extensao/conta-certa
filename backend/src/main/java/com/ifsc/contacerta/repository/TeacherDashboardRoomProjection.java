package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.model.Grade;

import java.time.Instant;
import java.util.UUID;

public interface TeacherDashboardRoomProjection {

	UUID getId();

	String getName();

	Grade getGrade();

	long getStudentCount();

	Instant getArchivedAt();

	Instant getLastActivityAt();
}
