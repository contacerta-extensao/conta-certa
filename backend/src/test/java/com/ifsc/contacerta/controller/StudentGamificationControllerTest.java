package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.gamification.AchievementResponse;
import com.ifsc.contacerta.dto.gamification.RankingEntryResponse;
import com.ifsc.contacerta.dto.gamification.RankingResponse;
import com.ifsc.contacerta.model.AchievementCode;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.StudentGamificationService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
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

class StudentGamificationControllerTest {

	@Test
	void deveExporRankingComPosicaoPropria() throws Exception {
		StudentGamificationService service = mock(StudentGamificationService.class);
		UUID studentId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		RankingEntryResponse peer = new RankingEntryResponse(1, UUID.randomUUID(), "Ana S.", 500, 9, 6, false);
		RankingEntryResponse me = new RankingEntryResponse(37, studentId, "Luiz M.", 120, 3, 2, true);
		when(service.ranking(studentId, roomId, 0, 20))
				.thenReturn(new RankingResponse(List.of(peer), me, 0, 20, 48, 3));
		var mockMvc = mockMvc(service, studentId);

		mockMvc.perform(get("/student/rooms/{roomId}/ranking", roomId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].displayName").value("Ana S."))
				.andExpect(jsonPath("$.me.position").value(37))
				.andExpect(jsonPath("$.totalElements").value(48))
				.andExpect(jsonPath("$.totalPages").value(3));
		verify(service).ranking(studentId, roomId, 0, 20);
	}

	@Test
	void deveExporCatalogoDeConquistas() throws Exception {
		StudentGamificationService service = mock(StudentGamificationService.class);
		UUID studentId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		Instant unlockedAt = Instant.parse("2026-08-29T12:00:00Z");
		when(service.achievements(studentId, roomId)).thenReturn(List.of(
				new AchievementResponse(
						AchievementCode.FIRST_PASS, "Primeira aprovação", "Aprove uma lição nesta sala.",
						"pi pi-check-circle", true, unlockedAt, 1, 1
				)
		));
		var mockMvc = mockMvc(service, studentId);

		mockMvc.perform(get("/student/rooms/{roomId}/achievements", roomId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("FIRST_PASS"))
				.andExpect(jsonPath("$[0].icon").value("pi pi-check-circle"))
				.andExpect(jsonPath("$[0].progressCurrent").value(1))
				.andExpect(jsonPath("$[0].progressTarget").value(1))
				.andExpect(jsonPath("$[0].unlocked").value(true))
				.andExpect(jsonPath("$[0].unlockedAt").value("2026-08-29T12:00:00Z"));
	}

	@Test
	void deveRepassarPaginacaoExplicita() throws Exception {
		StudentGamificationService service = mock(StudentGamificationService.class);
		UUID studentId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		when(service.ranking(studentId, roomId, 2, 50))
				.thenReturn(new RankingResponse(List.of(), null, 2, 50, 0, 0));

		mockMvc(service, studentId).perform(get("/student/rooms/{roomId}/ranking?page=2&size=50", roomId))
				.andExpect(status().isOk());

		verify(service).ranking(studentId, roomId, 2, 50);
	}

	private org.springframework.test.web.servlet.MockMvc mockMvc(StudentGamificationService service, UUID studentId) {
		return MockMvcBuilders.standaloneSetup(new StudentGamificationController(service))
				.setCustomArgumentResolvers(resolver(new CurrentUser(studentId, Role.STUDENT, UUID.randomUUID())))
				.build();
	}

	private HandlerMethodArgumentResolver resolver(CurrentUser user) {
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
				return user;
			}
		};
	}
}
