package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.media.MediaViewResponse;
import com.ifsc.contacerta.dto.media.MediaViewsPageResponse;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.MediaViewType;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.MaterialRepository;
import com.ifsc.contacerta.repository.MediaViewRepository;
import com.ifsc.contacerta.repository.MediaViewerProjection;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Visualizações de uma mídia do acervo do professor.
 *
 * A mídia pertence ao professor, não à sala, então a listagem cobre todas as
 * salas dele em que a mídia foi publicada.
 */
@Service
@RequiredArgsConstructor
public class TeacherMediaViewService {

	private final UserRepository userRepository;
	private final VideoRepository videoRepository;
	private final MaterialRepository materialRepository;
	private final MediaViewRepository viewRepository;

	@Transactional(readOnly = true)
	public MediaViewsPageResponse views(
			UUID teacherId,
			MediaViewType mediaType,
			UUID mediaId,
			int page,
			int size
	) {
		requireActiveTeacher(teacherId);
		requireOwnedMedia(teacherId, mediaType, mediaId);
		if (page < 0 || size < 1 || size > 100) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Page must be non-negative and size must be between 1 and 100."
			);
		}
		PageRequest pageable = PageRequest.of(page, size);
		Page<MediaViewerProjection> viewers = mediaType == MediaViewType.VIDEO
				? viewRepository.findVideoViewers(mediaId, teacherId, pageable)
				: viewRepository.findMaterialViewers(mediaId, teacherId, pageable);
		return MediaViewsPageResponse.from(viewers.map(this::toResponse));
	}

	private MediaViewResponse toResponse(MediaViewerProjection viewer) {
		return new MediaViewResponse(
				viewer.getStudentId(),
				viewer.getFullName(),
				viewer.getRegistrationNumber(),
				viewer.getFirstViewedAt(),
				viewer.getLastViewedAt()
		);
	}

	private void requireOwnedMedia(UUID teacherId, MediaViewType mediaType, UUID mediaId) {
		if (mediaType == MediaViewType.VIDEO) {
			videoRepository.findByIdAndTeacherId(mediaId, teacherId)
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VIDEO_NOT_FOUND", "Video was not found."));
			return;
		}
		materialRepository.findByIdAndTeacherId(mediaId, teacherId)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND, "MATERIAL_NOT_FOUND", "Material was not found."
				));
	}

	private User requireActiveTeacher(UUID teacherId) {
		User teacher = userRepository.findById(teacherId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND", "Teacher was not found."));
		if (teacher.getRole() != Role.TEACHER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "TEACHER_REQUIRED", "A teacher account is required.");
		}
		if (teacher.getStatus() != AccountStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Teacher account is inactive.");
		}
		return teacher;
	}
}
