package com.ifsc.contacerta.model;

import java.util.Arrays;

public enum ReportStudentSort {

	FULL_NAME("fullName", "u.full_name"),
	XP("xp", "coalesce(rsp.total_xp, 0)"),
	STARS("stars", "coalesce(rsp.total_best_stars, 0)"),
	COMPLETED_LESSONS("completedLessons", "coalesce(rsp.passed_assignment_count, 0)"),
	LAST_ACTIVITY_AT("lastActivityAt", "rsp.last_activity_at"),
	ATTEMPT_COUNT("attemptCount", "coalesce(metrics.attempt_count, 0)"),
	AVERAGE_SCORE_PERCENT("averageScorePercent", "coalesce(metrics.average_score, 0)"),
	BEST_SCORE_PERCENT("bestScorePercent", "coalesce(metrics.best_score, 0)");

	private final String property;
	private final String expression;

	ReportStudentSort(String property, String expression) {
		this.property = property;
		this.expression = expression;
	}

	public String expression() {
		return expression;
	}

	public static ReportStudentSort fromProperty(String property) {
		return Arrays.stream(values())
				.filter(value -> value.property.equals(property))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported student report sort: " + property));
	}
}
