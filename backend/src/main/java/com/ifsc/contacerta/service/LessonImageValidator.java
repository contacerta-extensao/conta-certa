package com.ifsc.contacerta.service;

import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.ValidatedUpload;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Imagens do Markdown da lição — PNG, JPEG ou WebP de até 5 MiB (§7.4 da spec).
 *
 * Extensão, `Content-Type` e assinatura do arquivo precisam concordar: só o
 * nome ou só o cabeçalho enviado pelo navegador seriam declarações de quem
 * envia, não do conteúdo.
 */
@Component
public class LessonImageValidator {

	public static final long MAX_BYTES = 5L * 1024 * 1024;

	private static final String PNG = "image/png";
	private static final String JPEG = "image/jpeg";
	private static final String WEBP = "image/webp";
	private static final byte[] PNG_SIGNATURE = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
	};
	private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] RIFF_SIGNATURE = "RIFF".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] WEBP_SIGNATURE = "WEBP".getBytes(StandardCharsets.US_ASCII);

	public ValidatedUpload validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw error(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_MEDIA", "An image is required.");
		}
		if (file.getSize() > MAX_BYTES) {
			throw error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Image exceeds the 5 MiB limit.");
		}

		String fileName = safeFileName(file.getOriginalFilename());
		String expectedContentType = expectedContentType(extension(fileName));
		if (!expectedContentType.equals(file.getContentType())) {
			throw unsupported();
		}

		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException exception) {
			throw error(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_MEDIA", "Image could not be read.");
		}
		if (!hasExpectedSignature(expectedContentType, content)) {
			throw unsupported();
		}
		return new ValidatedUpload(fileName, expectedContentType, content);
	}

	private String safeFileName(String original) {
		if (original == null) {
			throw unsupported();
		}
		String normalized = original.replace('\\', '/');
		normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
				.replace("\r", "")
				.replace("\n", "")
				.trim();
		if (normalized.isEmpty() || normalized.length() > 255) {
			throw unsupported();
		}
		return normalized;
	}

	private String extension(String fileName) {
		int separator = fileName.lastIndexOf('.');
		return separator < 0 ? "" : fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
	}

	private String expectedContentType(String extension) {
		return switch (extension) {
			case "png" -> PNG;
			case "jpg", "jpeg" -> JPEG;
			case "webp" -> WEBP;
			default -> throw unsupported();
		};
	}

	private boolean hasExpectedSignature(String contentType, byte[] content) {
		return switch (contentType) {
			case PNG -> startsWith(content, PNG_SIGNATURE);
			case JPEG -> startsWith(content, JPEG_SIGNATURE);
			case WEBP -> isWebp(content);
			default -> false;
		};
	}

	private boolean startsWith(byte[] content, byte[] signature) {
		return content.length >= signature.length
				&& Arrays.equals(Arrays.copyOf(content, signature.length), signature);
	}

	/** RIFF container: `RIFF`, quatro bytes de tamanho e o formato `WEBP`. */
	private boolean isWebp(byte[] content) {
		return content.length >= 12
				&& startsWith(content, RIFF_SIGNATURE)
				&& Arrays.equals(Arrays.copyOfRange(content, 8, 12), WEBP_SIGNATURE);
	}

	private ApiException unsupported() {
		return error(
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE",
				"Only PNG, JPEG, and WebP images are accepted."
		);
	}

	private ApiException error(HttpStatus status, String code, String message) {
		return new ApiException(status, code, message);
	}
}
