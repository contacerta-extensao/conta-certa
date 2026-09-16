package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.AuditEvent;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.AuditAction;
import com.ifsc.contacerta.model.AuditTargetType;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventRepositoryTest extends PostgresIntegrationTest {

	@Autowired
	private AuditEventRepository auditEventRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void persisteEventoComAutorAcaoRecursoEHorario() {
		User actor = userRepository.save(new User(
				Role.ADMIN,
				AccountStatus.ACTIVE,
				"Administrador",
				"admin-audit@example.com",
				null,
				null
		));
		UUID targetId = UUID.randomUUID();
		Instant occurredAt = Instant.parse("2026-09-16T12:00:00Z");

		AuditEvent saved = auditEventRepository.saveAndFlush(new AuditEvent(
				actor.getId(),
				AuditAction.INSTITUTION_CREATED,
				AuditTargetType.INSTITUTION,
				targetId,
				occurredAt
		));

		AuditEvent persisted = auditEventRepository.findById(saved.getId()).orElseThrow();
		assertThat(persisted.getActorUserId()).isEqualTo(actor.getId());
		assertThat(persisted.getAction()).isEqualTo(AuditAction.INSTITUTION_CREATED);
		assertThat(persisted.getTargetType()).isEqualTo(AuditTargetType.INSTITUTION);
		assertThat(persisted.getTargetId()).isEqualTo(targetId);
		assertThat(persisted.getOccurredAt()).isEqualTo(occurredAt);
	}
}
