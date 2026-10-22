package com.ifsc.contacerta.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Convite de professor feito por outro professor. Nao ha campo de instituicao:
 * o colega convidado entra sempre na instituicao de quem convida.
 */
public record InviteColleagueRequest(
		@NotBlank @Size(max = 160) String fullName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 80) String registrationNumber
) {
}
