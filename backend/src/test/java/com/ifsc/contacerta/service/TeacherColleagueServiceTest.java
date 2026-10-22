package com.ifsc.contacerta.service;

import com.ifsc.contacerta.dto.admin.CreateTeacherRequest;
import com.ifsc.contacerta.dto.teacher.InviteColleagueRequest;
import com.ifsc.contacerta.dto.teacher.TeacherColleagueResponse;
import com.ifsc.contacerta.entity.Institution;
import com.ifsc.contacerta.entity.User;
import com.ifsc.contacerta.exception.ApiException;
import com.ifsc.contacerta.model.AccountStatus;
import com.ifsc.contacerta.model.Role;
import com.ifsc.contacerta.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherColleagueServiceTest {

	@Mock UserRepository userRepository;
	@Mock AdminTeacherService adminTeacherService;
	@InjectMocks TeacherColleagueService service;

	private final Institution institution = new Institution("Alfa", "12345678000195", "a@example.com", "+5548999999999", true);

	@Test
	void convidaColegaNaInstituicaoDeQuemConvida() {
		User inviter = new User(Role.TEACHER, AccountStatus.ACTIVE, "Bia", "bia@example.com", "MAT-0", institution);
		User invited = new User(Role.TEACHER, AccountStatus.PENDING, "Ana Souza", "ana@example.com", "MAT-1", institution);
		when(userRepository.findById(inviter.getId())).thenReturn(Optional.of(inviter));
		when(adminTeacherService.create(eq(inviter.getId()), any())).thenReturn(invited);

		TeacherColleagueResponse response = service.invite(inviter.getId(),
				new InviteColleagueRequest("Ana Souza", "ana@example.com", "MAT-1"));

		ArgumentCaptor<CreateTeacherRequest> captor = ArgumentCaptor.forClass(CreateTeacherRequest.class);
		verify(adminTeacherService).create(eq(inviter.getId()), captor.capture());
		assertThat(captor.getValue().institutionId()).isEqualTo(institution.getId());
		assertThat(response.status()).isEqualTo(AccountStatus.PENDING);
		assertThat(response.email()).isEqualTo("ana@example.com");
	}

	@Test
	void recusaConviteDeQuemNaoEProfessor() {
		User student = new User(Role.STUDENT, AccountStatus.ACTIVE, "Caio", "caio@example.com", null, institution);
		when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

		assertThatThrownBy(() -> service.invite(student.getId(), new InviteColleagueRequest("Ana", "ana@example.com", "MAT-1")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "TEACHER_REQUIRED");
		verify(adminTeacherService, never()).create(any(), any());
	}

	@Test
	void recusaConviteDeProfessorInativo() {
		User inactive = new User(Role.TEACHER, AccountStatus.INACTIVE, "Bia", "bia@example.com", "MAT-0", institution);
		when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> service.invite(inactive.getId(), new InviteColleagueRequest("Ana", "ana@example.com", "MAT-1")))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "ACCOUNT_INACTIVE");
		verify(adminTeacherService, never()).create(any(), any());
	}

	@Test
	void listaApenasProfessoresDaMesmaInstituicao() {
		User teacher = new User(Role.TEACHER, AccountStatus.ACTIVE, "Bia", "bia@example.com", "MAT-0", institution);
		when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
		when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(teacher)));

		var page = service.list(teacher.getId(), null, null, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(TeacherColleagueResponse::fullName).containsExactly("Bia");
	}
}
