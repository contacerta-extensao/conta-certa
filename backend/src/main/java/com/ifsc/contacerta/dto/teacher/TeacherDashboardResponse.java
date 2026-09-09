package com.ifsc.contacerta.dto.teacher;

import java.util.List;

/**
 * Cartões-resumo do professor.
 *
 * {@code recentAttemptCount} conta as tentativas finalizadas nos últimos sete
 * dias em salas do professor; {@code recentRooms} traz as salas mais recentes
 * já com o total de alunos ativos e a última atividade da turma.
 */
public record TeacherDashboardResponse(
		long roomCount,
		long activeRoomCount,
		long archivedRoomCount,
		long studentCount,
		long lessonCount,
		long publishedLessonCount,
		long draftLessonCount,
		long recentAttemptCount,
		List<TeacherDashboardRoomResponse> recentRooms
) {
}
