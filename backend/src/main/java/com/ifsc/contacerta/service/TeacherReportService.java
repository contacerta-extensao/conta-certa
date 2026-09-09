package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.report.ReportPeriod;
import com.ifsc.contacerta.dto.report.TeacherReportOverviewResponse;
import com.ifsc.contacerta.dto.report.TeacherReportStudentResponse;
import com.ifsc.contacerta.dto.report.TeacherReportAttemptResponse;
import com.ifsc.contacerta.dto.report.TeacherReportRankingResponse;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.mapper.TeacherReportCsvMapper;
import com.ifsc.contacerta.model.ReportFilter;
import com.ifsc.contacerta.model.ReportStudentSort;
import com.ifsc.contacerta.repository.TeacherReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherReportService {

	private static final int CSV_PAGE_SIZE = 100;
	private static final int CSV_MAX_PAGES = 100;

	private final TeacherReportFilterFactory filterFactory;
	private final TeacherReportQueryRepository queryRepository;
	private final TeacherReportCsvMapper csvMapper;
	private final Clock clock;

	@Transactional(readOnly = true)
	public TeacherReportOverviewResponse overview(
			UUID teacherId,
			UUID roomId,
			UUID lessonId,
			ReportPeriod period,
			Instant from,
			Instant to
	) {
		ReportFilter filter = filterFactory.create(teacherId, roomId, lessonId, period, from, to);
		return queryRepository.overview(filter, clock.instant());
	}

	@Transactional(readOnly = true)
	public Page<TeacherReportStudentResponse> students(
			UUID teacherId,
			UUID roomId,
			UUID lessonId,
			ReportPeriod period,
			Instant from,
			Instant to,
			int page,
			int size,
			String sort,
			String direction
	) {
		requirePageBounds(page, size);
		Sort.Direction sortDirection = studentSortDirection(sort, direction);
		ReportFilter filter = filterFactory.create(teacherId, roomId, lessonId, period, from, to);
		return queryRepository.students(filter, PageRequest.of(page, size, Sort.by(sortDirection, sort)));
	}

	@Transactional(readOnly = true)
	public Page<TeacherReportAttemptResponse> attempts(
			UUID teacherId,
			UUID roomId,
			UUID studentId,
			UUID lessonId,
			ReportPeriod period,
			Instant from,
			Instant to,
			int page,
			int size,
			String direction
	) {
		requirePageBounds(page, size);
		Sort.Direction sortDirection = sortDirection(direction, "Unsupported report sort direction.");
		ReportFilter filter = filterFactory.create(teacherId, roomId, lessonId, period, from, to);
		filterFactory.requireActiveStudent(roomId, studentId);
		PageRequest pageable = PageRequest.of(
				page, size, Sort.by(sortDirection, "submittedAt")
		);
		return queryRepository.attempts(filter, studentId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<TeacherReportRankingResponse> ranking(
			UUID teacherId,
			UUID roomId,
			UUID lessonId,
			ReportPeriod period,
			Instant from,
			Instant to,
			int page,
			int size
	) {
		requirePageBounds(page, size);
		ReportFilter filter = filterFactory.create(teacherId, roomId, lessonId, period, from, to);
		return queryRepository.ranking(filter, PageRequest.of(page, size));
	}

	/**
	 * CSV do recorte atual, com uma linha por aluno.
	 *
	 * As linhas saem na mesma ordem padrão da aba Alunos (XP decrescente) e são
	 * lidas em páginas para que uma sala grande não seja carregada de uma vez.
	 */
	@Transactional(readOnly = true)
	public byte[] exportCsv(
			UUID teacherId,
			UUID roomId,
			UUID lessonId,
			ReportPeriod period,
			Instant from,
			Instant to
	) {
		ReportFilter filter = filterFactory.create(teacherId, roomId, lessonId, period, from, to);
		Sort sort = Sort.by(Sort.Direction.DESC, "xp");
		List<TeacherReportStudentResponse> rows = new ArrayList<>();
		int page = 0;
		Page<TeacherReportStudentResponse> current;
		do {
			current = queryRepository.students(filter, PageRequest.of(page, CSV_PAGE_SIZE, sort));
			rows.addAll(current.getContent());
			page++;
		} while (page < current.getTotalPages() && page < CSV_MAX_PAGES);
		return csvMapper.toCsv(rows);
	}

	private void requirePageBounds(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw badRequest("Page must be non-negative and size must be between 1 and 100.");
		}
	}

	private ApiException badRequest(String detail) {
		return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", detail);
	}

	private Sort.Direction studentSortDirection(String sort, String direction) {
		try {
			ReportStudentSort.fromProperty(sort);
			return Sort.Direction.fromString(direction);
		} catch (IllegalArgumentException exception) {
			throw badRequest("Unsupported report sort or direction.");
		}
	}

	private Sort.Direction sortDirection(String direction, String detail) {
		try {
			return Sort.Direction.fromString(direction);
		} catch (IllegalArgumentException exception) {
			throw badRequest(detail);
		}
	}
}
