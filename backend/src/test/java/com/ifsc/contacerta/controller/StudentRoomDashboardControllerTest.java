package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.studentdashboard.StudentNextLessonResponse;
import com.ifsc.contacerta.dto.studentdashboard.StudentRoomDashboardResponse;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.RoomMembershipService;
import com.ifsc.contacerta.service.StudentRoomDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentRoomDashboardControllerTest {

	@Test
	void deveExporDashboardDaSalaParaAlunoAutenticado() throws Exception {
		RoomMembershipService membershipService = mock(RoomMembershipService.class);
		StudentRoomDashboardService dashboardService = mock(StudentRoomDashboardService.class);
		UUID studentId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		UUID lessonId = UUID.randomUUID();
		when(dashboardService.dashboard(studentId, roomId)).thenReturn(new StudentRoomDashboardResponse(
				null, 50, 2, 150, 50, 50, 5, 12, 2, 4, 3L, 27,
				new StudentNextLessonResponse(assignmentId, lessonId, "Juros compostos", 3, null),
				List.of(),
				null
		));
		var mockMvc = MockMvcBuilders.standaloneSetup(new StudentRoomController(membershipService, dashboardService))
				.setCustomArgumentResolvers(resolver(new CurrentUser(studentId, Role.STUDENT, UUID.randomUUID())))
				.build();

		mockMvc.perform(get("/student/rooms/{roomId}/dashboard", roomId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpTotal").value(150))
				.andExpect(jsonPath("$.level").value(2))
				.andExpect(jsonPath("$.levelProgressPercent").value(50))
				.andExpect(jsonPath("$.xpToNextLevel").value(50))
				.andExpect(jsonPath("$.starsTotal").value(5))
				.andExpect(jsonPath("$.starsPossible").value(12))
				.andExpect(jsonPath("$.lessonsCompleted").value(2))
				.andExpect(jsonPath("$.lessonsTotal").value(4))
				.andExpect(jsonPath("$.progressPercent").value(50))
				.andExpect(jsonPath("$.rankingPosition").value(3))
				.andExpect(jsonPath("$.rankingParticipants").value(27))
				.andExpect(jsonPath("$.nextLesson.title").value("Juros compostos"))
				.andExpect(jsonPath("$.recentAchievements").isArray());
		verify(dashboardService).dashboard(studentId, roomId);
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
