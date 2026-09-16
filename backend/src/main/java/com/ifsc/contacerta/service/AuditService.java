package com.ifsc.contacerta.service;

import com.ifsc.contacerta.entity.AuditEvent;
import com.ifsc.contacerta.model.AuditAction;
import com.ifsc.contacerta.model.AuditTargetType;
import com.ifsc.contacerta.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditEventRepository auditEventRepository;
	private final Clock clock;

	public void record(
			UUID actorUserId,
			AuditAction action,
			AuditTargetType targetType,
			UUID targetId
	) {
		auditEventRepository.save(new AuditEvent(
				actorUserId,
				action,
				targetType,
				targetId,
				Instant.now(clock)
		));
	}
}
