package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.report.ReportMetricsResponse;
import com.ifsc.contacerta.dto.report.ReportPeriod;
import com.ifsc.contacerta.dto.report.TeacherReportOverviewResponse;
import com.ifsc.contacerta.dto.report.TeacherReportStudentResponse;
import com.ifsc.contacerta.dto.report.TeacherReportAttemptResponse;
import com.ifsc.contacerta.dto.report.TeacherReportRankingResponse;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.mapper.TeacherReportCsvMapper;
import com.ifsc.contacerta.model.ReportFilter;
import com.ifsc.contacerta.repository.TeacherReportQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeacherReportServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final TeacherReportCsvMapper csvMapper = new TeacherReportCsvMapper();

	@Test
	void deveConsultarOverviewComFiltroValidado() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportQueryRepository queryRepository = mock(TeacherReportQueryRepository.class);
		TeacherReportService service = new TeacherReportService(filterFactory, queryRepository, csvMapper, CLOCK);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ReportFilter filter = new ReportFilter(roomId, null, null, null);
		TeacherReportOverviewResponse expected = new TeacherReportOverviewResponse(
				new ReportMetricsResponse(
						0, 0, 0, 0, null, null, null, new BigDecimal("0.00"), new BigDecimal("0.00")
				),
				List.of(), List.of(), List.of(), NOW
		);
		when(filterFactory.create(teacherId, roomId, null, ReportPeriod.ALL, null, null)).thenReturn(filter);
		when(queryRepository.overview(filter, NOW)).thenReturn(expected);

		TeacherReportOverviewResponse result = service.overview(
				teacherId, roomId, null, ReportPeriod.ALL, null, null
		);

		assertThat(result).isSameAs(expected);
	}

	@Test
	void deveConsultarAlunosComPaginacaoEOrdenacaoValidadas() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportQueryRepository queryRepository = mock(TeacherReportQueryRepository.class);
		TeacherReportService service = new TeacherReportService(filterFactory, queryRepository, csvMapper, CLOCK);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ReportFilter filter = new ReportFilter(roomId, null, null, null);
		PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "xp"));
		Page<TeacherReportStudentResponse> expected = new PageImpl<>(List.of(), pageable, 0);
		when(filterFactory.create(teacherId, roomId, null, ReportPeriod.ALL, null, null)).thenReturn(filter);
		when(queryRepository.students(filter, pageable)).thenReturn(expected);

		Page<TeacherReportStudentResponse> result = service.students(
				teacherId, roomId, null, ReportPeriod.ALL, null, null,
				0, 20, "xp", "desc"
		);

		assertThat(result).isSameAs(expected);
	}

	@Test
	void deveRejeitarPaginacaoEOrdenacaoInvalidas() {
		TeacherReportService service = new TeacherReportService(
				mock(TeacherReportFilterFactory.class), mock(TeacherReportQueryRepository.class),
				new TeacherReportCsvMapper(), CLOCK
		);

		assertBadRequest(() -> service.students(
				UUID.randomUUID(), UUID.randomUUID(), null, null, null, null,
				0, 101, "xp", "desc"
		));
		assertBadRequest(() -> service.students(
				UUID.randomUUID(), UUID.randomUUID(), null, null, null, null,
				0, 20, "unknown", "desc"
		));
		assertBadRequest(() -> service.students(
				UUID.randomUUID(), UUID.randomUUID(), null, null, null, null,
				0, 20, "xp", "sideways"
		));
	}

	@Test
	void deveConsultarTentativasSomenteAposValidarAlunoAtivo() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportQueryRepository queryRepository = mock(TeacherReportQueryRepository.class);
		TeacherReportService service = new TeacherReportService(filterFactory, queryRepository, csvMapper, CLOCK);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		ReportFilter filter = new ReportFilter(roomId, null, null, null);
		PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "submittedAt"));
		Page<TeacherReportAttemptResponse> expected = new PageImpl<>(List.of(), pageable, 0);
		when(filterFactory.create(teacherId, roomId, null, ReportPeriod.ALL, null, null)).thenReturn(filter);
		when(queryRepository.attempts(filter, studentId, pageable)).thenReturn(expected);

		Page<TeacherReportAttemptResponse> result = service.attempts(
				teacherId, roomId, studentId, null, ReportPeriod.ALL, null, null,
				0, 20, "desc"
		);

		assertThat(result).isSameAs(expected);
	}

	@Test
	void deveConsultarRankingComFiltroValidado() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportQueryRepository queryRepository = mock(TeacherReportQueryRepository.class);
		TeacherReportService service = new TeacherReportService(filterFactory, queryRepository, csvMapper, CLOCK);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ReportFilter filter = new ReportFilter(roomId, null, null, null);
		PageRequest pageable = PageRequest.of(0, 20);
		Page<TeacherReportRankingResponse> expected = new PageImpl<>(List.of(), pageable, 0);
		when(filterFactory.create(teacherId, roomId, null, ReportPeriod.ALL, null, null)).thenReturn(filter);
		when(queryRepository.ranking(filter, pageable)).thenReturn(expected);

		Page<TeacherReportRankingResponse> result = service.ranking(
				teacherId, roomId, null, ReportPeriod.ALL, null, null, 0, 20
		);

		assertThat(result).isSameAs(expected);
	}

	@Test
	void devePropagarFalhaInternaDaCriacaoDoFiltroDeAlunos() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportService service = new TeacherReportService(
				filterFactory, mock(TeacherReportQueryRepository.class), new TeacherReportCsvMapper(), CLOCK
		);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		IllegalArgumentException failure = new IllegalArgumentException("falha interna");
		when(filterFactory.create(teacherId, roomId, null, null, null, null)).thenThrow(failure);

		assertThatThrownBy(() -> service.students(
				teacherId, roomId, null, null, null, null,
				0, 20, "xp", "desc"
		)).isSameAs(failure);
	}

	@Test
	void devePropagarFalhaInternaDaCriacaoDoFiltroDeTentativas() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportService service = new TeacherReportService(
				filterFactory, mock(TeacherReportQueryRepository.class), new TeacherReportCsvMapper(), CLOCK
		);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		IllegalArgumentException failure = new IllegalArgumentException("falha interna");
		when(filterFactory.create(teacherId, roomId, null, null, null, null)).thenThrow(failure);

		assertThatThrownBy(() -> service.attempts(
				teacherId, roomId, studentId, null, null, null, null,
				0, 20, "desc"
		)).isSameAs(failure);
	}

	@Test
	void deveGerarCsvPercorrendoTodasAsPaginasDeAlunos() {
		TeacherReportFilterFactory filterFactory = mock(TeacherReportFilterFactory.class);
		TeacherReportQueryRepository queryRepository = mock(TeacherReportQueryRepository.class);
		TeacherReportService service = new TeacherReportService(
				filterFactory, queryRepository, csvMapper, CLOCK
		);
		UUID teacherId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ReportFilter filter = new ReportFilter(roomId, null, null, null);
		Sort sort = Sort.by(Sort.Direction.DESC, "xp");
		TeacherReportStudentResponse first = student("Aluno Um");
		TeacherReportStudentResponse second = student("Aluno Dois");
		when(filterFactory.create(teacherId, roomId, null, ReportPeriod.ALL, null, null)).thenReturn(filter);
		when(queryRepository.students(filter, PageRequest.of(0, 100, sort)))
				.thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 100, sort), 150));
		when(queryRepository.students(filter, PageRequest.of(1, 100, sort)))
				.thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 100, sort), 150));

		String csv = new String(
				service.exportCsv(teacherId, roomId, null, ReportPeriod.ALL, null, null),
				StandardCharsets.UTF_8
		);

		assertThat(csv.lines()).hasSize(3);
		assertThat(csv).contains("Aluno Um").contains("Aluno Dois");
	}

	private TeacherReportStudentResponse student(String fullName) {
		return new TeacherReportStudentResponse(
				UUID.randomUUID(), fullName, "S1", "aluno@example.com",
				0, 0, 1, 0, 0, 0, 0, null, null, null
		);
	}

	private void assertBadRequest(Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(exception.getCode()).isEqualTo("BAD_REQUEST");
				});
	}
}
