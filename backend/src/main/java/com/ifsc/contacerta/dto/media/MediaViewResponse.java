package com.ifsc.contacerta.dto.media;

import java.time.Instant;
import java.util.UUID;

/** Uma abertura de mídia, do ponto de vista do professor: um aluno, não um clique. */
public record MediaViewResponse(
		UUID studentId,
		String fullName,
		String registrationNumber,
		Instant firstViewedAt,
		Instant lastViewedAt
) {
}
