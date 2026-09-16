package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.admin.AdminTeacherResponse;
import com.ifsc.contacerta.dto.admin.CreateTeacherRequest;
import com.ifsc.contacerta.dto.institution.InstitutionSummaryResponse;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.AdminTeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTeacherControllerTest {
	private AdminTeacherService service;
	private MockMvc mockMvc;
	private UUID adminId;

	@BeforeEach
	void setUp() {
		service = mock(AdminTeacherService.class);
		adminId = UUID.randomUUID();
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminTeacherController(service))
				.setCustomArgumentResolvers(currentUserResolver())
				.build();
	}

	@Test
	void criaProfessorComLocation() throws Exception {
		UUID id = UUID.randomUUID();
		User teacher = new User(Role.TEACHER, AccountStatus.PENDING, "Ana Souza", "ana@example.com", "MAT-1", null);
		AdminTeacherResponse response = response(id);
		when(service.create(eq(adminId), any(CreateTeacherRequest.class))).thenReturn(teacher);
		when(service.get(teacher.getId())).thenReturn(response);

		mockMvc.perform(post("/admin/teachers")
				.contentType("application/json")
				.content("{\"fullName\":\"Ana Souza\",\"email\":\"ana@example.com\",\"registrationNumber\":\"MAT-1\",\"institutionId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/admin/teachers/" + id));
	}

	private HandlerMethodArgumentResolver currentUserResolver() {
		return new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.getParameterType() == CurrentUser.class;
			}

			@Override
			public Object resolveArgument(
					MethodParameter parameter,
					ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest,
					WebDataBinderFactory binderFactory
			) {
				return new CurrentUser(adminId, Role.ADMIN, UUID.randomUUID());
			}
		};
	}

	@Test
	void listaProfessoresComFiltro() throws Exception {
		when(service.list(eq("ana"), eq(AccountStatus.ACTIVE), any(), any()))
				.thenReturn(new PageImpl<>(List.of(response(UUID.randomUUID())), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/admin/teachers").param("search", "ana").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(service).list(eq("ana"), eq(AccountStatus.ACTIVE), eq(null), any());
	}

	private AdminTeacherResponse response(UUID id) {
		return new AdminTeacherResponse(id, "Ana Souza", "ana@example.com", "MAT-1",
				new InstitutionSummaryResponse(UUID.randomUUID(), "Alfa", "12345678000195", "a@example.com", "+5548999999999", true),
				AccountStatus.PENDING, false, 0, null, null, null);
	}
}
