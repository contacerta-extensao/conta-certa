package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.admin.CreateTeacherRequest;
import com.ifsc.contacerta.dto.teacher.InviteColleagueRequest;
import com.ifsc.contacerta.dto.teacher.TeacherColleagueResponse;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.mapper.TeacherColleagueMapper;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.UserRepository;
import com.ifsc.contacerta.specification.TeacherSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Professores da mesma instituicao. Um professor ativo pode listar os colegas e
 * convidar novos; o convite segue o mesmo fluxo do admin (conta PENDING + e-mail
 * para definir a senha), mas a instituicao e sempre a de quem convida.
 */
@Service
@RequiredArgsConstructor
public class TeacherColleagueService {
	private final UserRepository userRepository;
	private final AdminTeacherService adminTeacherService;

	@Transactional(readOnly = true)
	public Page<TeacherColleagueResponse> list(UUID teacherId, String search, AccountStatus status, Pageable pageable) {
		UUID institutionId = requireActiveTeacher(teacherId).getInstitution().getId();
		return userRepository.findAll(TeacherSpecification.filtered(search, status, institutionId), pageable)
				.map(TeacherColleagueMapper::toResponse);
	}

	@Transactional
	public TeacherColleagueResponse invite(UUID teacherId, InviteColleagueRequest request) {
		UUID institutionId = requireActiveTeacher(teacherId).getInstitution().getId();
		User created = adminTeacherService.create(teacherId, new CreateTeacherRequest(
				request.fullName(), request.email(), request.registrationNumber(), institutionId));
		// `create` devolve a instancia antes do merge; a gerenciada tem `createdAt` preenchido.
		return TeacherColleagueMapper.toResponse(userRepository.findById(created.getId()).orElse(created));
	}

	private User requireActiveTeacher(UUID teacherId) {
		User teacher = userRepository.findById(teacherId).orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND", "Teacher was not found."
		));
		if (teacher.getRole() != Role.TEACHER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "TEACHER_REQUIRED", "A teacher account is required.");
		}
		if (teacher.getStatus() != AccountStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Teacher account is inactive.");
		}
		if (teacher.getInstitution() == null) {
			throw new ApiException(HttpStatus.CONFLICT, "TEACHER_INSTITUTION_REQUIRED", "Teacher has no institution.");
		}
		return teacher;
	}
}
