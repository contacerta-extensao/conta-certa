package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.MediaView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

public interface MediaViewRepository extends JpaRepository<MediaView, UUID> {
	Optional<MediaView> findByStudentIdAndRoomIdAndVideoId(UUID studentId, UUID roomId, UUID videoId);
	Optional<MediaView> findByStudentIdAndRoomIdAndMaterialId(UUID studentId, UUID roomId, UUID materialId);

	@Modifying
	@Query(value = """
			insert into media_views
			(id, student_id, room_id, media_type, video_id, material_id, first_viewed_at, last_viewed_at, view_count)
			values (:id, :studentId, :roomId, 'VIDEO', :videoId, null, :viewedAt, :viewedAt, 1)
			on conflict (student_id, room_id, video_id) where video_id is not null do update
			set last_viewed_at = excluded.last_viewed_at, view_count = media_views.view_count + 1
			""", nativeQuery = true)
	void upsertVideo(
			@Param("id") UUID id,
			@Param("studentId") UUID studentId,
			@Param("roomId") UUID roomId,
			@Param("videoId") UUID videoId,
			@Param("viewedAt") Instant viewedAt
	);

	@Modifying
	@Query(value = """
			insert into media_views
			(id, student_id, room_id, media_type, video_id, material_id, first_viewed_at, last_viewed_at, view_count)
			values (:id, :studentId, :roomId, 'MATERIAL', null, :materialId, :viewedAt, :viewedAt, 1)
			on conflict (student_id, room_id, material_id) where material_id is not null do update
			set last_viewed_at = excluded.last_viewed_at, view_count = media_views.view_count + 1
			""", nativeQuery = true)
	void upsertMaterial(
			@Param("id") UUID id,
			@Param("studentId") UUID studentId,
			@Param("roomId") UUID roomId,
			@Param("materialId") UUID materialId,
			@Param("viewedAt") Instant viewedAt
	);

	/**
	 * Quem abriu o vídeo, uma linha por aluno.
	 *
	 * O mesmo vídeo pode estar em mais de uma sala do professor, por isso as
	 * visualizações do aluno são agregadas: {@code firstViewedAt} é a primeira
	 * abertura em qualquer sala dele e {@code lastViewedAt}, a mais recente.
	 */
	@Query(value = """
			select student.id as studentId, student.fullName as fullName,
				student.registrationNumber as registrationNumber,
				min(view.firstViewedAt) as firstViewedAt, max(view.lastViewedAt) as lastViewedAt
			from MediaView view
			join view.student student
			where view.video.id = :videoId and view.room.teacher.id = :teacherId
			group by student.id, student.fullName, student.registrationNumber
			order by max(view.lastViewedAt) desc, student.fullName asc
			""", countQuery = """
			select count(distinct view.student.id)
			from MediaView view
			where view.video.id = :videoId and view.room.teacher.id = :teacherId
			""")
	Page<MediaViewerProjection> findVideoViewers(
			@Param("videoId") UUID videoId,
			@Param("teacherId") UUID teacherId,
			Pageable pageable
	);

	/** Quem abriu o material, uma linha por aluno. Ver {@link #findVideoViewers}. */
	@Query(value = """
			select student.id as studentId, student.fullName as fullName,
				student.registrationNumber as registrationNumber,
				min(view.firstViewedAt) as firstViewedAt, max(view.lastViewedAt) as lastViewedAt
			from MediaView view
			join view.student student
			where view.material.id = :materialId and view.room.teacher.id = :teacherId
			group by student.id, student.fullName, student.registrationNumber
			order by max(view.lastViewedAt) desc, student.fullName asc
			""", countQuery = """
			select count(distinct view.student.id)
			from MediaView view
			where view.material.id = :materialId and view.room.teacher.id = :teacherId
			""")
	Page<MediaViewerProjection> findMaterialViewers(
			@Param("materialId") UUID materialId,
			@Param("teacherId") UUID teacherId,
			Pageable pageable
	);
}
