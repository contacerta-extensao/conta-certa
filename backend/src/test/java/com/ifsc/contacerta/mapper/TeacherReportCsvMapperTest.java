package com.ifsc.contacerta.mapper;

import com.ifsc.contacerta.dto.report.TeacherReportStudentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherReportCsvMapperTest {

	private final TeacherReportCsvMapper mapper = new TeacherReportCsvMapper();

	@Test
	void deveEscreverCabecalhoEUmaLinhaPorAluno() {
		String csv = csv(List.of(student(
				"Aluno Um", "S1", "um@example.com", 2, new BigDecimal("60.00"), new BigDecimal("80.00"),
				Instant.parse("2026-09-05T14:30:00Z")
		)));

		assertThat(csv).startsWith("﻿Aluno;Matrícula;E-mail;Tentativas;Média (%);Melhor (%);"
				+ "Lições concluídas;Total de lições;XP;Estrelas;Nível;Última atividade\r\n");
		assertThat(csv.lines().toList()).hasSize(2);
		assertThat(csv).contains("Aluno Um;S1;um@example.com;2;60,00;80,00;2;3;150;5;2;05/09/2026 11:30");
	}

	@Test
	void deveDeixarCamposVaziosQuandoNaoHaValor() {
		String csv = csv(List.of(student("Aluno Dois", null, "dois@example.com", 0, null, null, null)));

		assertThat(csv).contains("Aluno Dois;;dois@example.com;0;;;2;3;150;5;2;\r\n");
	}

	@Test
	void deveProtegerSeparadorEAspasNoTexto() {
		String csv = csv(List.of(student(
				"Silva; Maria \"Bibi\"", "S3", "maria@example.com", 1,
				new BigDecimal("100.00"), new BigDecimal("100.00"), null
		)));

		assertThat(csv).contains("\"Silva; Maria \"\"Bibi\"\"\";S3;");
	}

	@Test
	void deveEscreverSomenteCabecalhoSemAlunos() {
		assertThat(csv(List.of()).lines().toList()).hasSize(1);
	}

	private String csv(List<TeacherReportStudentResponse> students) {
		return new String(mapper.toCsv(students), StandardCharsets.UTF_8);
	}

	private TeacherReportStudentResponse student(
			String fullName,
			String registrationNumber,
			String email,
			long attemptCount,
			BigDecimal averageScorePercent,
			BigDecimal bestScorePercent,
			Instant lastActivityAt
	) {
		return new TeacherReportStudentResponse(
				UUID.randomUUID(), fullName, registrationNumber, email,
				150, 5, 2, 2, 2, 3, attemptCount,
				averageScorePercent, bestScorePercent, lastActivityAt
		);
	}
}
