package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.model.ContentStatus;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialRepositoryTest extends PostgresIntegrationTest {

	@Autowired private MaterialRepository materialRepository;

	@Test
	void deveListarMateriaisSemFiltrosOpcionais() {
		var page = materialRepository.searchOwned(
				UUID.randomUUID(), ContentStatus.ARCHIVED, null, null, PageRequest.of(0, 20)
		);

		assertThat(page).isEmpty();
	}
}
