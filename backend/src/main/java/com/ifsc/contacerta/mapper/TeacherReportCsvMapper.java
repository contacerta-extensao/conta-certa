package com.ifsc.contacerta.mapper;

import com.ifsc.contacerta.dto.report.TeacherReportStudentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV do relatório por aluno.
 *
 * O arquivo é feito para ser aberto no Excel em português: separador `;`,
 * decimal com vírgula, BOM UTF-8 para os acentos e horário em
 * America/Sao_Paulo, o mesmo fuso que a tela mostra.
 */
@Component
public class TeacherReportCsvMapper {

	private static final char SEPARATOR = ';';
	private static final String LINE_BREAK = "\r\n";
	private static final String BOM = "﻿";
	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final List<String> HEADERS = List.of(
			"Aluno", "Matrícula", "E-mail", "Tentativas", "Média (%)", "Melhor (%)",
			"Lições concluídas", "Total de lições", "XP", "Estrelas", "Nível", "Última atividade"
	);

	public byte[] toCsv(List<TeacherReportStudentResponse> students) {
		StringBuilder csv = new StringBuilder(BOM);
		appendRow(csv, HEADERS);
		for (TeacherReportStudentResponse student : students) {
			appendRow(csv, List.of(
					text(student.fullName()),
					text(student.registrationNumber()),
					text(student.email()),
					String.valueOf(student.attemptCount()),
					decimal(student.averageScorePercent()),
					decimal(student.bestScorePercent()),
					String.valueOf(student.completedLessons()),
					String.valueOf(student.totalLessons()),
					String.valueOf(student.xp()),
					String.valueOf(student.stars()),
					String.valueOf(student.level()),
					timestamp(student.lastActivityAt())
			));
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private void appendRow(StringBuilder csv, List<String> values) {
		for (int index = 0; index < values.size(); index++) {
			if (index > 0) {
				csv.append(SEPARATOR);
			}
			csv.append(escape(values.get(index)));
		}
		csv.append(LINE_BREAK);
	}

	private String escape(String value) {
		if (value.indexOf(SEPARATOR) < 0 && value.indexOf('"') < 0
				&& value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
			return value;
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}

	private String text(String value) {
		return value == null ? "" : value;
	}

	private String decimal(BigDecimal value) {
		return value == null ? "" : value.toPlainString().replace('.', ',');
	}

	private String timestamp(Instant value) {
		return value == null ? "" : TIMESTAMP.format(value.atZone(ZONE));
	}
}
