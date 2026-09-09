package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.report.ReportMetricsResponse;
import com.ifsc.contacerta.dto.report.ReportPeriod;
import com.ifsc.contacerta.dto.report.ReportScoreBucketResponse;
import com.ifsc.contacerta.dto.report.TeacherReportOverviewResponse;
import com.ifsc.contacerta.dto.report.TeacherReportRankingResponse;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.TeacherReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherReportControllerTest {

	private TeacherReportService service;
	private MockMvc mockMvc;
	private CurrentUser currentUser;

	@BeforeEach
	void setUp() {
		service = mock(TeacherReportService.class);
		currentUser = new CurrentUser(UUID.randomUUID(), Role.TEACHER, UUID.randomUUID());
		mockMvc = MockMvcBuilders.standaloneSetup(new TeacherReportController(service))
				.setCustomArgumentResolvers(currentUserResolver())
				.build();
	}

	@Test
	void deveExporOverviewComPeriodoAll() throws Exception {
		UUID roomId = UUID.randomUUID();
		TeacherReportOverviewResponse response = new TeacherReportOverviewResponse(
				new ReportMetricsResponse(
						2, 1, 5, 4, new BigDecimal("75.00"), new BigDecimal("25.00"),
						new BigDecimal("50.00"), new BigDecimal("120.00"), new BigDecimal("3.00")
				),
				List.of(),
				List.of(new ReportScoreBucketResponse("90-100%", 1)),
				List.of(),
				Instant.parse("2026-09-08T12:00:00Z")
		);
		when(service.overview(currentUser.userId(), roomId, null,
				ReportPeriod.ALL, null, null)).thenReturn(response);

		mockMvc.perform(get("/teacher/reports/overview").param("roomId", roomId.toString()).param("period", "ALL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.metrics.studentCount").value(2))
				.andExpect(jsonPath("$.metrics.activeStudentCount").value(1))
				.andExpect(jsonPath("$.metrics.attemptCount").value(5))
				.andExpect(jsonPath("$.metrics.submittedAttemptCount").value(4))
				.andExpect(jsonPath("$.metrics.averageScorePercent").value(75.00))
				.andExpect(jsonPath("$.scoreDistribution[0].label").value("90-100%"))
				.andExpect(jsonPath("$.scoreDistribution[0].count").value(1))
				.andExpect(jsonPath("$.generatedAt").value("2026-09-08T12:00:00Z"));
	}

	@Test
	void deveExporAlunosComDefaultsDePaginacao() throws Exception {
		UUID roomId = UUID.randomUUID();
		PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "xp"));
		when(service.students(currentUser.userId(), roomId, null, null, null, null,
				0, 20, "xp", "desc")).thenReturn(new PageImpl<>(List.of(), pageable, 0));

		mockMvc.perform(get("/teacher/reports/students").param("roomId", roomId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void deveExporTentativasDoAluno() throws Exception {
		UUID roomId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "submittedAt"));
		when(service.attempts(currentUser.userId(), roomId, studentId, null, null, null, null,
				0, 20, "desc")).thenReturn(new PageImpl<>(List.of(), pageable, 0));

		mockMvc.perform(get("/teacher/reports/students/{studentId}/attempts", studentId)
					.param("roomId", roomId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void deveExporRankingCompleto() throws Exception {
		UUID roomId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		PageRequest pageable = PageRequest.of(0, 20);
		when(service.ranking(currentUser.userId(), roomId, null, null, null, null, 0, 20))
				.thenReturn(new PageImpl<>(List.of(
						new TeacherReportRankingResponse(1, studentId, "Aluno", "S1", "a@example.com", 10, 2, 3, null)
				), pageable, 1));

		mockMvc.perform(get("/teacher/reports/ranking").param("roomId", roomId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].position").value(1))
				.andExpect(jsonPath("$.content[0].studentId").value(studentId.toString()))
				.andExpect(jsonPath("$.content[0].xp").value(10))
				.andExpect(jsonPath("$.content[0].completedLessons").value(3))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void deveBaixarCsvComOsMesmosFiltros() throws Exception {
		UUID roomId = UUID.randomUUID();
		byte[] csv = "Aluno;XP\r\nAluno Um;150\r\n".getBytes(StandardCharsets.UTF_8);
		when(service.exportCsv(currentUser.userId(), roomId, null, ReportPeriod.ALL, null, null)).thenReturn(csv);

		mockMvc.perform(get("/teacher/reports/export.csv")
						.param("roomId", roomId.toString())
						.param("period", "ALL"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
				.andExpect(header().string(
						HttpHeaders.CONTENT_DISPOSITION,
						startsWith("attachment; filename=\"relatorio-conta-certa.csv\"")
				))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
				.andExpect(content().bytes(csv));
	}

	private HandlerMethodArgumentResolver currentUserResolver() {
		return new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.getParameterType().equals(CurrentUser.class);
			}

			@Override
			public Object resolveArgument(
					MethodParameter parameter,
					ModelAndViewContainer container,
					NativeWebRequest request,
					WebDataBinderFactory binderFactory
			) {
				return currentUser;
			}
		};
	}
}
