package com.ifsc.contacerta.dto.report;

import java.time.LocalDate;

/** Ponto da série diária: tentativas iniciadas e finalizadas no dia (UTC). */
public record ReportAttemptSeriesItemResponse(LocalDate date, long attempts, long submitted) { }
