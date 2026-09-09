package com.ifsc.contacerta.mapper;

import com.ifsc.contacerta.entity.Lesson;
import com.ifsc.contacerta.entity.LessonImage;
import com.ifsc.contacerta.entity.StoredFile;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LessonImageMapperTest {

	private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

	@Test
	void deveMontarUrlRelativaComPrefixoDaApi() {
		LessonImage image = image();

		assertThat(new LessonImageMapper("/api/v1").toResponse(image))
				.extracting("id", "url", "fileName")
				.containsExactly(image.getId(), "/api/v1/lesson-images/" + image.getId(), "grafico.png");
	}

	@Test
	void deveTolerarPrefixoAusenteOuComBarraFinal() {
		UUID imageId = UUID.randomUUID();

		assertThat(new LessonImageMapper(null).url(imageId)).isEqualTo("/lesson-images/" + imageId);
		assertThat(new LessonImageMapper("").url(imageId)).isEqualTo("/lesson-images/" + imageId);
		assertThat(new LessonImageMapper("/api/v1/").url(imageId)).isEqualTo("/api/v1/lesson-images/" + imageId);
	}

	private LessonImage image() {
		User teacher = new User(Role.TEACHER, AccountStatus.ACTIVE, "Professora", "prof@example.com", "P-1", null);
		Lesson lesson = new Lesson("Juros", null, "# Teoria", teacher);
		StoredFile file = new StoredFile(
				teacher, "grafico.png", "image/png", 3, "sha", new byte[]{1, 2, 3}, NOW
		);
		return new LessonImage(lesson, file, NOW);
	}
}
