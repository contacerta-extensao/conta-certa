package com.ifsc.contacerta.dto.report;

/** Faixa da distribuição de notas, já rotulada pelo servidor. */
public record ReportScoreBucketResponse(String label, long count) { }
