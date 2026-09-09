package com.ifsc.contacerta.dto.report;

import java.math.BigDecimal;

/**
 * Cartões de métrica do relatório.
 *
 * {@code studentCount} conta as matrículas ativas da sala e
 * {@code activeStudentCount}, apenas quem tentou alguma lição no recorte.
 * As médias e taxas são nulas quando não há tentativa no recorte — nunca zero,
 * que a tela leria como "média zero".
 */
public record ReportMetricsResponse(
		long studentCount,
		long activeStudentCount,
		long attemptCount,
		long submittedAttemptCount,
		BigDecimal averageScorePercent,
		BigDecimal passRatePercent,
		BigDecimal completionPercent,
		BigDecimal averageRoomXp,
		BigDecimal averageBestStars
) {
}
