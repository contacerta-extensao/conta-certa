package com.ifsc.contacerta.dto.report;

import java.time.Instant;
import java.util.List;

public record TeacherReportOverviewResponse(
		ReportMetricsResponse metrics,
		List<ReportAttemptSeriesItemResponse> attemptsOverTime,
		List<ReportScoreBucketResponse> scoreDistribution,
		List<ReportLessonCompletionResponse> lessonCompletion,
		Instant generatedAt
) { }
