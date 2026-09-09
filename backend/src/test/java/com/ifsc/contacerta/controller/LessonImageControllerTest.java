package com.ifsc.contacerta.controller;

import com.ifsc.contacerta.dto.lesson.LessonImageResponse;
import com.ifsc.contacerta.model.FileDownload;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.security.CurrentUser;
import com.ifsc.contacerta.service.LessonImageService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LessonImageControllerTest {

	private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	@Test
	void deveEnviarImagemEDevolverUrlPronta() throws Exception {
		LessonImageService service = mock(LessonImageService.class);
		UUID teacherId = UUID.randomUUID();
		UUID lessonId = UUID.randomUUID();
		UUID imageId = UUID.randomUUID();
		String url = "/api/v1/lesson-images/" + imageId;
		when(service.upload(any(UUID.class), any(UUID.class), any(MultipartFile.class)))
				.thenReturn(new LessonImageResponse(imageId, url, "grafico.png"));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherLessonImageController(service))
				.setCustomArgumentResolvers(resolver(new CurrentUser(teacherId, Role.TEACHER, UUID.randomUUID())))
				.build();

		mockMvc.perform(multipart("/teacher/lessons/{lessonId}/images", lessonId)
						.file("file", PNG))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, url))
				.andExpect(jsonPath("$.id").value(imageId.toString()))
				.andExpect(jsonPath("$.url").value(url))
				.andExpect(jsonPath("$.fileName").value("grafico.png"));
		verify(service).upload(any(UUID.class), any(UUID.class), any(MultipartFile.class));
	}

	@Test
	void deveServirImagemComCacheLongoESemAnexo() throws Exception {
		LessonImageService service = mock(LessonImageService.class);
		UUID imageId = UUID.randomUUID();
		when(service.content(imageId)).thenReturn(new FileDownload("grafico.png", "image/png", PNG.length, PNG));
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LessonImageController(service)).build();

		mockMvc.perform(get("/lesson-images/{imageId}", imageId))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(content().bytes(PNG));
	}

	private HandlerMethodArgumentResolver resolver(CurrentUser user) {
		return new HandlerMethodArgumentResolver() {
			@Override public boolean supportsParameter(MethodParameter parameter) {
				return parameter.getParameterType() == CurrentUser.class;
			}

			@Override public Object resolveArgument(
					MethodParameter parameter,
					ModelAndViewContainer container,
					NativeWebRequest request,
					WebDataBinderFactory factory
			) {
				return user;
			}
		};
	}
}
