package com.ifsc.contacerta.repository;

import java.util.UUID;

public interface RankingRowProjection {

	long getPosition();

	UUID getStudentId();

	String getFullName();

	int getXp();

	int getStars();

	int getLessonsPassed();
}
