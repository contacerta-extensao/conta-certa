package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.shared.PageResponse;
import com.ifsc.contacerta.dto.teacher.InviteColleagueRequest;
import com.ifsc.contacerta.dto.teacher.TeacherColleagueResponse;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.TeacherColleagueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/teacher/colleagues")
@RequiredArgsConstructor
public class TeacherColleagueController {
	private static final Set<String> SORT_FIELDS = Set.of("fullName", "createdAt", "status");
	private final TeacherColleagueService service;
	private final PageableFactory pageableFactory = new PageableFactory();

	@GetMapping
	public PageResponse<TeacherColleagueResponse> list(
			@AuthenticationPrincipal CurrentUser currentUser,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) AccountStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "fullName,asc") String sort
	) {
		return PageResponse.from(service.list(currentUser.userId(), search, status,
				pageableFactory.create(page, size, sort, SORT_FIELDS, "INVALID_TEACHER_SORT")));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TeacherColleagueResponse invite(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody InviteColleagueRequest request
	) {
		return service.invite(currentUser.userId(), request);
	}
}
