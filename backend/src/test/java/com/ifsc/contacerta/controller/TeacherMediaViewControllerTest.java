package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.media.MediaViewResponse;
import com.ifsc.contacerta.dto.media.MediaViewsPageResponse;
import com.ifsc.contacerta.model.MediaViewType;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.TeacherMediaViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherMediaViewControllerTest {

	private TeacherMediaViewService service;
	private MockMvc mockMvc;
	private CurrentUser currentUser;

	@BeforeEach
	void setUp() {
		service = mock(TeacherMediaViewService.class);
		currentUser = new CurrentUser(UUID.randomUUID(), Role.TEACHER, UUID.randomUUID());
		mockMvc = MockMvcBuilders.standaloneSetup(new TeacherMediaViewController(service))
				.setCustomArgumentResolvers(resolver())
				.build();
	}

	@Test
	void deveExporVisualizacoesComTotalDeAlunos() throws Exception {
		UUID mediaId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		when(service.views(currentUser.userId(), MediaViewType.VIDEO, mediaId, 0, 20))
				.thenReturn(new MediaViewsPageResponse(
						List.of(new MediaViewResponse(
								studentId, "Aluno Um", "S1",
								Instant.parse("2026-09-01T10:00:00Z"),
								Instant.parse("2026-09-05T11:00:00Z")
						)),
						0, 20, 1, 1, 1
				));

		mockMvc.perform(get("/teacher/media/{mediaType}/{mediaId}/views", "VIDEO", mediaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].studentId").value(studentId.toString()))
				.andExpect(jsonPath("$.content[0].fullName").value("Aluno Um"))
				.andExpect(jsonPath("$.content[0].registrationNumber").value("S1"))
				.andExpect(jsonPath("$.content[0].firstViewedAt").value("2026-09-01T10:00:00Z"))
				.andExpect(jsonPath("$.content[0].lastViewedAt").value("2026-09-05T11:00:00Z"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.totalViewers").value(1));
	}

	@Test
	void deveRepassarTipoDeMidiaEPaginacaoExplicita() throws Exception {
		UUID mediaId = UUID.randomUUID();
		when(service.views(currentUser.userId(), MediaViewType.MATERIAL, mediaId, 2, 50))
				.thenReturn(new MediaViewsPageResponse(List.of(), 2, 50, 0, 0, 0));

		mockMvc.perform(get("/teacher/media/{mediaType}/{mediaId}/views", "MATERIAL", mediaId)
						.param("page", "2")
						.param("size", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		verify(service).views(currentUser.userId(), MediaViewType.MATERIAL, mediaId, 2, 50);
	}

	private HandlerMethodArgumentResolver resolver() {
		return new HandlerMethodArgumentResolver() {
			@Override public boolean supportsParameter(MethodParameter parameter) {
				return parameter.getParameterType() == CurrentUser.class;
			}

			@Override public Object resolveArgument(
					MethodParameter parameter,
					ModelAndViewContainer container,
					NativeWebRequest request,
					WebDataBinderFactory factory
			) {
				return currentUser;
			}
		};
	}
}
