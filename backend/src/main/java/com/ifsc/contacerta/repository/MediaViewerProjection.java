package com.ifsc.contacerta.repository;

import java.time.Instant;
import java.util.UUID;

public interface MediaViewerProjection {

	UUID getStudentId();

	String getFullName();

	String getRegistrationNumber();

	Instant getFirstViewedAt();

	Instant getLastViewedAt();
}
