package com.ifsc.contacerta.model;

import java.util.Arrays;

/**
 * Arquivo já validado, pronto para ser armazenado.
 *
 * O conteúdo é copiado na entrada e na saída para que o array não seja
 * compartilhado com quem construiu o registro.
 */
public record ValidatedUpload(String fileName, String contentType, byte[] content) {

	public ValidatedUpload {
		content = Arrays.copyOf(content, content.length);
	}

	@Override
	public byte[] content() {
		return Arrays.copyOf(content, content.length);
	}
}
