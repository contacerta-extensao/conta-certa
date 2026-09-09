package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.studentlesson.AttemptHistoryResponse;
import com.ifsc.contacerta.dto.studentlesson.StudentLessonDetailResponse;
import com.ifsc.contacerta.dto.studentlesson.StudentLessonPathResponse;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.StudentLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentLessonController {
	private final StudentLessonService lessonService;

	@GetMapping("/rooms/{roomId}/lessons")
	public List<StudentLessonPathResponse> path(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID roomId) {
		return lessonService.path(currentUser.userId(), roomId);
	}

	@GetMapping("/rooms/{roomId}/lessons/{lessonId}")
	public StudentLessonDetailResponse detail(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID roomId, @PathVariable UUID lessonId) {
		return lessonService.detail(currentUser.userId(), roomId, lessonId);
	}

	@GetMapping("/rooms/{roomId}/lessons/{lessonId}/attempts")
	public List<AttemptHistoryResponse> history(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID roomId,
			@PathVariable UUID lessonId
	) {
		return lessonService.history(currentUser.userId(), roomId, lessonId);
	}
}
