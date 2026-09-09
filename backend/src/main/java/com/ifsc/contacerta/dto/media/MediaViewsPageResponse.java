package com.ifsc.contacerta.dto.media;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Página de visualizações.
 *
 * {@code totalViewers} é o total de alunos que abriram a mídia — o mesmo que
 * {@code totalElements}, porque cada linha é um aluno, e existe para a tela
 * poder anunciar o total sem depender da paginação.
 */
public record MediaViewsPageResponse(
		List<MediaViewResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		long totalViewers
) {

	public static MediaViewsPageResponse from(Page<MediaViewResponse> page) {
		return new MediaViewsPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.getTotalElements()
		);
	}
}
