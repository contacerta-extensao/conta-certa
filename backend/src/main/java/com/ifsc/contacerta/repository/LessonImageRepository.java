package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.LessonImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LessonImageRepository extends JpaRepository<LessonImage, UUID> {

	/** Leitura pública da imagem: carrega o arquivo junto, sem passar pelo dono. */
	@EntityGraph(attributePaths = "file")
	Optional<LessonImage> findWithFileById(UUID id);
}
