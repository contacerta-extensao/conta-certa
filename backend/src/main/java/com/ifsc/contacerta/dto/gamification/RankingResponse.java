package com.ifsc.contacerta.dto.gamification;

import java.util.List;

public record RankingResponse(
		List<RankingEntryResponse> content,
		RankingEntryResponse me,
		int page,
		int size,
		long totalElements,
		int totalPages
) {}
