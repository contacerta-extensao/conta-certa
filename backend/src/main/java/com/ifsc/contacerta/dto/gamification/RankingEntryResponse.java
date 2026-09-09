package com.ifsc.contacerta.dto.gamification;

import java.util.UUID;

public record RankingEntryResponse(
		long position,
		UUID studentId,
		String displayName,
		int xp,
		int stars,
		int lessonsPassed,
		boolean me
) {}
