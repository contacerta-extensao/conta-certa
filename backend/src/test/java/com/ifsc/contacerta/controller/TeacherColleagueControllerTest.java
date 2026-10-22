package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.teacher.InviteColleagueRequest;
import com.ifsc.contacerta.dto.teacher.TeacherColleagueResponse;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.TeacherColleagueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherColleagueControllerTest {

	private TeacherColleagueService service;
	private MockMvc mockMvc;
	private CurrentUser currentUser;

	@BeforeEach
	void setUp() {
		service = mock(TeacherColleagueService.class);
		currentUser = new CurrentUser(UUID.randomUUID(), Role.TEACHER, UUID.randomUUID());
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
		);
		mockMvc = MockMvcBuilders.standaloneSetup(new TeacherColleagueController(service))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void listaColegasDoProfessorAutenticado() throws Exception {
		var colleague = response(AccountStatus.ACTIVE);
		when(service.list(eq(currentUser.userId()), isNull(), isNull(), any()))
				.thenReturn(new PageImpl<>(List.of(colleague), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/teacher/colleagues"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].fullName").value("Ana Souza"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void convidaColegaERetorna201() throws Exception {
		var request = new InviteColleagueRequest("Ana Souza", "ana@example.com", "MAT-1");
		when(service.invite(currentUser.userId(), request)).thenReturn(response(AccountStatus.PENDING));

		mockMvc.perform(post("/teacher/colleagues")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Ana Souza","email":"ana@example.com","registrationNumber":"MAT-1"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void rejeitaConviteSemEmail() throws Exception {
		mockMvc.perform(post("/teacher/colleagues")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Ana Souza","email":"","registrationNumber":"MAT-1"}
								"""))
				.andExpect(status().isBadRequest());
		verify(service, never()).invite(any(), any());
	}

	private static TeacherColleagueResponse response(AccountStatus status) {
		return new TeacherColleagueResponse(UUID.randomUUID(), "Ana Souza", "ana@example.com", "MAT-1",
				status, Instant.parse("2026-10-01T10:00:00Z"));
	}
}
