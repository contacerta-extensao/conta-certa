package com.ifsc.contacerta.entity;

import com.ifsc.contacerta.model.AuditAction;
import com.ifsc.contacerta.model.AuditTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

	@Id
	private UUID id;

	@Column(name = "actor_user_id", nullable = false, updatable = false)
	private UUID actorUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 48)
	private AuditAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, updatable = false, length = 32)
	private AuditTargetType targetType;

	@Column(name = "target_id", nullable = false, updatable = false)
	private UUID targetId;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	public AuditEvent(
			UUID actorUserId,
			AuditAction action,
			AuditTargetType targetType,
			UUID targetId,
			Instant occurredAt
	) {
		this.id = UUID.randomUUID();
		this.actorUserId = actorUserId;
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.occurredAt = occurredAt;
	}
}
