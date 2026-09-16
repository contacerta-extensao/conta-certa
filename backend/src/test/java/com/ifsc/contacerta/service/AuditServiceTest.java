package com.ifsc.contacerta.service;

import com.ifsc.contacerta.entity.AuditEvent;
import com.ifsc.contacerta.model.AuditAction;
import com.ifsc.contacerta.model.AuditTargetType;
import com.ifsc.contacerta.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

	@Mock
	private AuditEventRepository auditEventRepository;

	private final Clock clock = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);

	private AuditService auditService;

	@BeforeEach
	void setUp() {
		auditService = new AuditService(auditEventRepository, clock);
	}

	@Test
	void registraAutorAcaoRecursoEHorario() {
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		auditService.record(actorId, AuditAction.ROOM_CODE_REGENERATED, AuditTargetType.ROOM, targetId);

		ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(auditEventRepository).save(eventCaptor.capture());
		AuditEvent event = eventCaptor.getValue();
		assertThat(event.getId()).isNotNull();
		assertThat(event.getActorUserId()).isEqualTo(actorId);
		assertThat(event.getAction()).isEqualTo(AuditAction.ROOM_CODE_REGENERATED);
		assertThat(event.getTargetType()).isEqualTo(AuditTargetType.ROOM);
		assertThat(event.getTargetId()).isEqualTo(targetId);
		assertThat(event.getOccurredAt()).isEqualTo(Instant.parse("2026-09-16T12:00:00Z"));
	}
}
