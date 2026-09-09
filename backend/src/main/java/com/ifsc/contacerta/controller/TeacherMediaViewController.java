package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.media.MediaViewsPageResponse;
import com.ifsc.contacerta.model.MediaViewType;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.TeacherMediaViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/teacher/media/{mediaType}/{mediaId}/views")
@RequiredArgsConstructor
public class TeacherMediaViewController {

	private final TeacherMediaViewService service;

	@GetMapping
	public MediaViewsPageResponse views(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable MediaViewType mediaType,
			@PathVariable UUID mediaId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return service.views(currentUser.userId(), mediaType, mediaId, page, size);
	}
}
