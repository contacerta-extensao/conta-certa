package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.gamification.AchievementResponse;
import com.ifsc.contacerta.dto.gamification.RankingResponse;
import com.ifsc.contacerta.dto.studentdashboard.StudentNextLessonResponse;
import com.ifsc.contacerta.dto.studentdashboard.StudentRoomDashboardResponse;
import com.ifsc.contacerta.dto.studentlesson.StudentLessonPathResponse;
import com.ifsc.contacerta.entity.RoomMembership;
import com.ifsc.contacerta.entity.RoomStudentProgress;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.mapper.RoomMapper;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AttemptAvailabilityStatus;
import com.ifsc.contacerta.model.MembershipStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.RoomMembershipRepository;
import com.ifsc.contacerta.repository.RoomStudentProgressRepository;
import com.ifsc.contacerta.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentRoomDashboardService {

	private static final int XP_PER_LEVEL = 100;
	private static final int MAX_STARS_PER_LESSON = 3;

	private final UserRepository userRepository;
	private final RoomMembershipRepository membershipRepository;
	private final RoomStudentProgressRepository progressRepository;
	private final StudentLessonService lessonService;
	private final StudentGamificationService gamificationService;
	private final StudentFinancialTipService financialTipService;

	@Transactional(readOnly = true)
	public StudentRoomDashboardResponse dashboard(UUID studentId, UUID roomId) {
		User student = requireStudent(studentId);
		RoomMembership membership = requireMembership(student, roomId);
		List<StudentLessonPathResponse> path = lessonService.path(studentId, roomId);
		RoomStudentProgress progress = progressRepository.findByRoomIdAndStudentId(roomId, studentId).orElse(null);

		int lessonsTotal = path.size();
		int totalXp = progress == null ? 0 : progress.getTotalXp();
		int lessonsCompleted = progress == null ? 0 : progress.getPassedAssignmentCount();
		int progressPercent = progressPercent(lessonsCompleted, lessonsTotal);
		RankingResponse ranking = gamificationService.ranking(studentId, roomId, 0, 1);

		return new StudentRoomDashboardResponse(
				RoomMapper.toStudentResponse(membership.getRoom(), membership.getStatus(), progressPercent),
				progressPercent,
				progress == null ? 1 : progress.getLevel(),
				totalXp,
				totalXp % XP_PER_LEVEL,
				XP_PER_LEVEL - totalXp % XP_PER_LEVEL,
				progress == null ? 0 : progress.getTotalBestStars(),
				lessonsTotal * MAX_STARS_PER_LESSON,
				lessonsCompleted,
				lessonsTotal,
				rankingPosition(progress, ranking),
				ranking.totalElements(),
				nextLesson(path),
				recentAchievements(studentId, roomId),
				financialTipService.currentTip()
		);
	}

	private User requireStudent(UUID studentId) {
		User student = userRepository.findById(studentId)
				.orElseThrow(() -> roomNotFound());
		if (student.getRole() != Role.STUDENT) {
			throw new ApiException(HttpStatus.FORBIDDEN, "STUDENT_REQUIRED", "A student account is required.");
		}
		if (student.getStatus() != AccountStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Student account is inactive.");
		}
		return student;
	}

	private RoomMembership requireMembership(User student, UUID roomId) {
		RoomMembership membership = membershipRepository.findByRoomIdAndStudentId(roomId, student.getId())
				.filter(candidate -> candidate.getStatus() == MembershipStatus.ACTIVE)
				.orElseThrow(this::roomNotFound);
		if (!membership.getRoom().getInstitution().getId().equals(student.getInstitution().getId())) {
			throw roomNotFound();
		}
		return membership;
	}

	/** Sem nenhuma lição finalizada não há classificação a exibir. */
	private Long rankingPosition(RoomStudentProgress progress, RankingResponse ranking) {
		return progress == null || progress.getCompletedAssignmentCount() == 0
				? null
				: ranking.me().position();
	}

	private int progressPercent(int passedLessons, int totalLessons) {
		return totalLessons == 0 ? 0 : passedLessons * 100 / totalLessons;
	}

	private StudentNextLessonResponse nextLesson(List<StudentLessonPathResponse> path) {
		return path.stream().filter(lesson -> lesson.availability() == AttemptAvailabilityStatus.IN_PROGRESS).findFirst()
				.or(() -> path.stream().filter(lesson -> lesson.availability() == AttemptAvailabilityStatus.AVAILABLE).findFirst())
				.or(() -> path.stream().filter(this::canRetry).findFirst())
				.map(lesson -> new StudentNextLessonResponse(
						lesson.assignmentId(), lesson.lessonId(), lesson.title(), lesson.order(), lesson.activeAttemptId()
				))
				.orElse(null);
	}

	private boolean canRetry(StudentLessonPathResponse lesson) {
		return lesson.availability() == AttemptAvailabilityStatus.FAILED
				&& (lesson.rules().attemptsRemaining() == null || lesson.rules().attemptsRemaining() > 0);
	}

	private List<AchievementResponse> recentAchievements(UUID studentId, UUID roomId) {
		return gamificationService.achievements(studentId, roomId).stream()
				.filter(AchievementResponse::unlocked)
				.sorted(Comparator.comparing(
						AchievementResponse::unlockedAt,
						Comparator.nullsLast(Comparator.reverseOrder())
				).thenComparing(achievement -> achievement.code().name()))
				.limit(3)
				.toList();
	}

	private ApiException roomNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Room was not found.");
	}
}
