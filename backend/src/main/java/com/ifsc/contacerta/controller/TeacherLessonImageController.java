package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.lesson.LessonImageResponse;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.LessonImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/teacher/lessons/{lessonId}/images")
@RequiredArgsConstructor
public class TeacherLessonImageController {

	private final LessonImageService service;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<LessonImageResponse> upload(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID lessonId,
			@RequestPart(name = "file", required = false) MultipartFile file
	) {
		LessonImageResponse response = service.upload(currentUser.userId(), lessonId, file);
		return ResponseEntity.created(URI.create(response.url())).body(response);
	}
}
