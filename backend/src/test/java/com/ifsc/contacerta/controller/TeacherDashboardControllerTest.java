package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.teacher.TeacherDashboardResponse;
import com.ifsc.contacerta.dto.teacher.TeacherDashboardRoomResponse;
import com.ifsc.contacerta.model.Grade;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.TeacherDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherDashboardControllerTest {

	private TeacherDashboardService service;
	private MockMvc mockMvc;
	private CurrentUser currentUser;

	@BeforeEach
	void setUp() {
		service = mock(TeacherDashboardService.class);
		currentUser = new CurrentUser(UUID.randomUUID(), Role.TEACHER, UUID.randomUUID());
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
		);
		mockMvc = MockMvcBuilders.standaloneSetup(new TeacherDashboardController(service))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deveExporEnvelopeCompletoUsandoIdDoProfessorAutenticado() throws Exception {
		UUID roomId = UUID.randomUUID();
		when(service.get(currentUser.userId())).thenReturn(new TeacherDashboardResponse(
				4, 3, 1, 86, 12, 9, 3, 17,
				List.of(new TeacherDashboardRoomResponse(
						roomId, "2º ano A", Grade.HIGH_SCHOOL_2, 18, false,
						Instant.parse("2026-09-07T10:00:00Z")
				))
		));

		mockMvc.perform(get("/teacher/dashboard"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "roomCount": 4,
						  "activeRoomCount": 3,
						  "archivedRoomCount": 1,
						  "studentCount": 86,
						  "lessonCount": 12,
						  "publishedLessonCount": 9,
						  "draftLessonCount": 3,
						  "recentAttemptCount": 17,
						  "recentRooms": [
						    {
						      "id": "%s",
						      "name": "2º ano A",
						      "grade": "HIGH_SCHOOL_2",
						      "studentCount": 18,
						      "archived": false,
						      "lastActivityAt": "2026-09-07T10:00:00Z"
						    }
						  ]
						}
						""".formatted(roomId), JsonCompareMode.STRICT));
	}
}
