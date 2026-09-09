package com.ifsc.contacerta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Imagem referenciada no Markdown de uma lição.
 *
 * Existe separada de {@link StoredFile} porque é ela que autoriza a leitura
 * pública da imagem: só arquivo apontado por uma linha desta tabela pode ser
 * servido sem token, e o identificador do arquivo em si nunca é exposto.
 */
@Entity
@Table(name = "lesson_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonImage {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "file_id", nullable = false, unique = true)
	private StoredFile file;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public LessonImage(Lesson lesson, StoredFile file, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.lesson = lesson;
		this.file = file;
		this.createdAt = createdAt;
	}
}
