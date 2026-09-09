package com.ifsc.contacerta.dto.gamification;

import com.ifsc.contacerta.model.AchievementCode;

import java.time.Instant;

public record AchievementResponse(
		AchievementCode code,
		String title,
		String description,
		String icon,
		boolean unlocked,
		Instant unlockedAt,
		int progressCurrent,
		int progressTarget
) {}
