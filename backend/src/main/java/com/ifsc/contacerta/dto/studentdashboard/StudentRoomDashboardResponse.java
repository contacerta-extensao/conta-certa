package com.ifsc.contacerta.dto.studentdashboard;

import com.ifsc.contacerta.dto.gamification.AchievementResponse;
import com.ifsc.contacerta.dto.room.StudentRoomResponse;

import java.util.List;

/**
 * Painel da sala do aluno.
 *
 * Todo número já chega calculado, inclusive {@code levelProgressPercent} e
 * {@code xpToNextLevel}: a tela não faz aritmética de progresso.
 * {@code rankingPosition} é nulo enquanto o aluno não finalizar nenhuma lição.
 */
public record StudentRoomDashboardResponse(
		StudentRoomResponse room,
		int progressPercent,
		int level,
		int xpTotal,
		int levelProgressPercent,
		int xpToNextLevel,
		int starsTotal,
		int starsPossible,
		int lessonsCompleted,
		int lessonsTotal,
		Long rankingPosition,
		long rankingParticipants,
		StudentNextLessonResponse nextLesson,
		List<AchievementResponse> recentAchievements,
		StudentFinancialTipResponse tipOfDay
) {
}
