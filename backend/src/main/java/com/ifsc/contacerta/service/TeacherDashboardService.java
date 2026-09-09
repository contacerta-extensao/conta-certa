package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.teacher.TeacherDashboardResponse;
import com.ifsc.contacerta.dto.teacher.TeacherDashboardRoomResponse;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AttemptStatus;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.MembershipStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.AttemptRepository;
import com.ifsc.contacerta.repository.LessonRepository;
import com.ifsc.contacerta.repository.RoomMembershipRepository;
import com.ifsc.contacerta.repository.RoomRepository;
import com.ifsc.contacerta.repository.TeacherDashboardRoomProjection;
import com.ifsc.contacerta.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

	private static final Duration RECENT_WINDOW = Duration.ofDays(7);
	private static final int RECENT_ROOM_LIMIT = 5;
	private static final List<AttemptStatus> FINALIZED = List.of(AttemptStatus.SUBMITTED, AttemptStatus.EXPIRED);

	private final UserRepository userRepository;
	private final RoomRepository roomRepository;
	private final RoomMembershipRepository membershipRepository;
	private final LessonRepository lessonRepository;
	private final AttemptRepository attemptRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public TeacherDashboardResponse get(UUID teacherId) {
		requireActiveTeacher(teacherId);
		return new TeacherDashboardResponse(
				roomRepository.countByTeacherId(teacherId),
				roomRepository.countByTeacherIdAndArchivedAtIsNull(teacherId),
				roomRepository.countByTeacherIdAndArchivedAtIsNotNull(teacherId),
				membershipRepository.countDistinctStudentsByTeacherId(teacherId),
				lessonRepository.countByTeacherId(teacherId),
				lessonRepository.countByTeacherIdAndStatus(teacherId, ContentStatus.PUBLISHED),
				lessonRepository.countByTeacherIdAndStatus(teacherId, ContentStatus.DRAFT),
				attemptRepository.countFinalizedByTeacherIdSince(
						teacherId, FINALIZED, clock.instant().minus(RECENT_WINDOW)
				),
				recentRooms(teacherId)
		);
	}

	private List<TeacherDashboardRoomResponse> recentRooms(UUID teacherId) {
		return roomRepository.findDashboardRooms(
						teacherId, MembershipStatus.ACTIVE, PageRequest.of(0, RECENT_ROOM_LIMIT)
				).stream()
				.map(this::toRoomCard)
				.toList();
	}

	private TeacherDashboardRoomResponse toRoomCard(TeacherDashboardRoomProjection room) {
		return new TeacherDashboardRoomResponse(
				room.getId(),
				room.getName(),
				room.getGrade(),
				room.getStudentCount(),
				room.getArchivedAt() != null,
				room.getLastActivityAt()
		);
	}

	private void requireActiveTeacher(UUID teacherId) {
		User teacher = userRepository.findById(teacherId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND", "Teacher was not found."
		));
		if (teacher.getRole() != Role.TEACHER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "TEACHER_REQUIRED", "A teacher account is required.");
		}
		if (teacher.getStatus() != AccountStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Teacher account is inactive.");
		}
	}
}
