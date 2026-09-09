package com.ifsc.contacerta.dto.lesson;

import java.util.UUID;

/**
 * Imagem enviada pelo editor de lição.
 *
 * {@code url} já vem pronta para ir dentro do Markdown, com o prefixo da API,
 * e é servida sem token — é ela que o navegador do aluno coloca no {@code src}.
 */
public record LessonImageResponse(UUID id, String url, String fileName) {
}
