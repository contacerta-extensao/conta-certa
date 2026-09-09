package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.shared.PageResponse;
import com.ifsc.contacerta.exception.GlobalExceptionHandler;
import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherLessonControllerTest {

	private LessonService lessonService;
	private MockMvc mockMvc;
	private UUID teacherId;

	@BeforeEach
	void setUp() {
		lessonService = mock(LessonService.class);
		teacherId = UUID.randomUUID();
		mockMvc = MockMvcBuilders.standaloneSetup(new TeacherLessonController(lessonService))
				.setCustomArgumentResolvers(currentUserResolver(new CurrentUser(teacherId, Role.TEACHER, UUID.randomUUID())))
				.setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC()))
				.build();
	}

	@Test
	void deveRepassarBuscaSituacaoEOrdenacaoPedidaPelaTela() throws Exception {
		when(lessonService.list(eq(teacherId), eq("juros"), eq(ContentStatus.PUBLISHED), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

		mockMvc.perform(get("/teacher/lessons")
						.param("search", "juros")
						.param("status", "PUBLISHED")
						.param("size", "100")
						.param("sort", "title,asc"))
				.andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(lessonService).list(eq(teacherId), eq("juros"), eq(ContentStatus.PUBLISHED), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
	}

	@Test
	void deveOrdenarPorCriacaoDecrescenteQuandoNadaEPedido() throws Exception {
		when(lessonService.list(eq(teacherId), any(), any(), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/teacher/lessons")).andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(lessonService).list(eq(teacherId), any(), any(), pageable.capture());
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Test
	void deveRejeitarCampoDeOrdenacaoForaDaLista() throws Exception {
		mockMvc.perform(get("/teacher/lessons").param("sort", "theoryMarkdown,asc"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("INVALID_LESSON_SORT"));
	}

	@Test
	void deveRejeitarTamanhoDePaginaForaDoLimite() throws Exception {
		mockMvc.perform(get("/teacher/lessons").param("size", "101"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("INVALID_PAGE"));
	}

	private HandlerMethodArgumentResolver currentUserResolver(CurrentUser currentUser) {
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
