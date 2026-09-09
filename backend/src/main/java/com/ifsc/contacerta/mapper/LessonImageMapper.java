package com.ifsc.contacerta.mapper;

import com.ifsc.contacerta.dto.lesson.LessonImageResponse;
import com.ifsc.contacerta.entity.LessonImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Monta a URL pública da imagem.
 *
 * A URL é relativa e inclui o prefixo da API, porque ela é gravada dentro do
 * Markdown da lição: um endereço absoluto ficaria preso ao host do ambiente em
 * que a imagem foi enviada.
 */
@Component
public class LessonImageMapper {

	private final String basePath;

	LessonImageMapper(@Value("${server.servlet.context-path:}") String contextPath) {
		this.basePath = contextPath == null ? "" : contextPath.replaceAll("/+$", "");
	}

	public LessonImageResponse toResponse(LessonImage image) {
		return new LessonImageResponse(image.getId(), url(image.getId()), image.getFile().getFileName());
	}

	public String url(UUID imageId) {
		return basePath + "/lesson-images/" + imageId;
	}
}
