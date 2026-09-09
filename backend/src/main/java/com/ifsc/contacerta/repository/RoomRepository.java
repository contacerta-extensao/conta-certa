package com.ifsc.contacerta.repository;

import com.ifsc.contacerta.entity.Room;
import com.ifsc.contacerta.model.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

	long countByTeacherId(UUID teacherId);

	long countByTeacherIdAndArchivedAtIsNull(UUID teacherId);

	long countByTeacherIdAndArchivedAtIsNotNull(UUID teacherId);

	Optional<Room> findByJoinCodeHash(String joinCodeHash);

	Optional<Room> findByJoinCodeHashAndInstitutionId(String joinCodeHash, UUID institutionId);

	Optional<Room> findByIdAndTeacherId(UUID id, UUID teacherId);

	boolean existsByJoinCodeHash(String joinCodeHash);

	boolean existsByTeacherIdAndName(UUID teacherId, String name);

	Page<Room> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

	@Query("""
			select room.id as id, room.name as name, room.grade as grade,
				(select count(membership) from RoomMembership membership
					where membership.room = room and membership.status = :activeStatus) as studentCount,
				room.archivedAt as archivedAt,
				(select max(progress.lastActivityAt) from RoomStudentProgress progress
					where progress.room = room) as lastActivityAt
			from Room room
			where room.teacher.id = :teacherId
			order by room.createdAt desc, room.id desc
			""")
	List<TeacherDashboardRoomProjection> findDashboardRooms(
			@Param("teacherId") UUID teacherId,
			@Param("activeStatus") MembershipStatus activeStatus,
			Pageable pageable
	);
}
