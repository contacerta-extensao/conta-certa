package com.ifsc.contacerta.dto.extraattempt;

import java.util.UUID;

/**
 * Concessão de tentativas extras.
 *
 * {@code extraAttemptsGranted} é o total já concedido ao aluno naquela
 * atribuição, e {@code attemptsAvailable} nulo significa "sem limite".
 */
public record ExtraAttemptGrantResponse(
		UUID id,
		UUID assignmentId,
		UUID studentId,
		int extraAttemptsGranted,
		long attemptsUsed,
		Long attemptsAvailable
) {
}
