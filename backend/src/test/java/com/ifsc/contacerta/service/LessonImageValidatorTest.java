package com.ifsc.contacerta.service;

import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.ValidatedUpload;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonImageValidatorTest {

	private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
	private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

	private final LessonImageValidator validator = new LessonImageValidator();

	@Test
	void deveAceitarPngJpegEWebp() {
		assertThat(validator.validate(file("grafico.png", "image/png", PNG)))
				.extracting(ValidatedUpload::fileName, ValidatedUpload::contentType)
				.containsExactly("grafico.png", "image/png");
		assertThat(validator.validate(file("foto.jpg", "image/jpeg", JPEG)).contentType())
				.isEqualTo("image/jpeg");
		assertThat(validator.validate(file("foto.jpeg", "image/jpeg", JPEG)).contentType())
				.isEqualTo("image/jpeg");
		assertThat(validator.validate(file("banner.webp", "image/webp", webp())).contentType())
				.isEqualTo("image/webp");
	}

	@Test
	void deveDescartarCaminhoDoNomeEnviado() {
		assertThat(validator.validate(file("C:\\fotos\\grafico.png", "image/png", PNG)).fileName())
				.isEqualTo("grafico.png");
	}

	@Test
	void deveRejeitarArquivoAusenteOuVazio() {
		assertError(() -> validator.validate(null), HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_MEDIA");
		assertError(
				() -> validator.validate(file("grafico.png", "image/png", new byte[0])),
				HttpStatus.UNPROCESSABLE_CONTENT,
				"INVALID_MEDIA"
		);
	}

	@Test
	void deveRejeitarImagemAcimaDoLimite() {
		byte[] content = new byte[(int) LessonImageValidator.MAX_BYTES + 1];
		System.arraycopy(PNG, 0, content, 0, PNG.length);

		assertError(
				() -> validator.validate(file("grande.png", "image/png", content)),
				HttpStatus.PAYLOAD_TOO_LARGE,
				"FILE_TOO_LARGE"
		);
	}

	@Test
	void deveRejeitarExtensaoNaoSuportada() {
		assertError(
				() -> validator.validate(file("aula.pdf", "application/pdf", PNG)),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE"
		);
		assertError(
				() -> validator.validate(file("semextensao", "image/png", PNG)),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE"
		);
	}

	@Test
	void deveRejeitarContentTypeQueNaoConfereComAExtensao() {
		assertError(
				() -> validator.validate(file("grafico.png", "image/jpeg", PNG)),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE"
		);
	}

	@Test
	void deveRejeitarConteudoQueNaoTemAssinaturaDaImagem() {
		assertError(
				() -> validator.validate(file("grafico.png", "image/png", "<script>".getBytes(StandardCharsets.UTF_8))),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE"
		);
		byte[] riffSemWebp = "RIFF____AVI ".getBytes(StandardCharsets.US_ASCII);
		assertError(
				() -> validator.validate(file("banner.webp", "image/webp", riffSemWebp)),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE"
		);
	}

	private byte[] webp() {
		return "RIFF____WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
	}

	private MockMultipartFile file(String name, String contentType, byte[] content) {
		return new MockMultipartFile("file", name, contentType, content);
	}

	private void assertError(Runnable action, HttpStatus status, String code) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(status);
					assertThat(exception.getCode()).isEqualTo(code);
				});
	}
}
