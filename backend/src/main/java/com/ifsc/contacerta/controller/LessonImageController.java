package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.model.FileDownload;
import com.ifsc.contacerta.service.LessonImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Leitura pública das imagens do Markdown das lições.
 *
 * Rota sem autenticação porque o navegador carrega a imagem por um
 * {@code <img src>}, que não envia o token. O identificador é um UUID aleatório
 * e serve apenas imagens de lição; nada mais do acervo é alcançável por aqui.
 */
@RestController
@RequiredArgsConstructor
public class LessonImageController {

	private final LessonImageService service;

	@GetMapping("/lesson-images/{imageId}")
	public ResponseEntity<byte[]> image(@PathVariable UUID imageId) {
		FileDownload image = service.content(imageId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.contentType()))
				.contentLength(image.sizeBytes())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
				.header("X-Content-Type-Options", "nosniff")
				.body(image.content());
	}
}
