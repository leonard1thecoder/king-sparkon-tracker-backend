package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.PrivilegeRepository;

@ExtendWith(MockitoExtension.class)
class PrivilegeServiceTest {

	@Mock
	private PrivilegeRepository privilegeRepository;

	@InjectMocks
	private PrivilegeService privilegeService;

	@Test
	void createPrivilegeReturnsExistingPrivilege() {
		Privilege existing = new Privilege(PrivilegeRole.Owner);
		when(privilegeRepository.findByName(PrivilegeRole.Owner)).thenReturn(Optional.of(existing));

		Privilege result = privilegeService.createPrivilege(PrivilegeRole.Owner);

		assertThat(result).isSameAs(existing);
		verify(privilegeRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createPrivilegeSavesMissingPrivilege() {
		when(privilegeRepository.findByName(PrivilegeRole.Worker)).thenReturn(Optional.empty());
		when(privilegeRepository.save(org.mockito.ArgumentMatchers.any(Privilege.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Privilege result = privilegeService.createPrivilege(PrivilegeRole.Worker);

		assertThat(result.getName()).isEqualTo(PrivilegeRole.Worker);
		verify(privilegeRepository).save(org.mockito.ArgumentMatchers.any(Privilege.class));
	}

	@Test
	void createPrivilegeRejectsMissingRole() {
		assertThatThrownBy(() -> privilegeService.createPrivilege(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Privilege role is required");
	}

	@Test
	void getPrivilegeReturnsPrivilege() {
		Privilege privilege = new Privilege(PrivilegeRole.Owner);
		when(privilegeRepository.findByName(PrivilegeRole.Owner)).thenReturn(Optional.of(privilege));

		Privilege result = privilegeService.getPrivilege(PrivilegeRole.Owner);

		assertThat(result).isSameAs(privilege);
	}

	@Test
	void getPrivilegeThrowsWhenMissing() {
		when(privilegeRepository.findByName(PrivilegeRole.Owner)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> privilegeService.getPrivilege(PrivilegeRole.Owner))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Privilege not found: Owner");
	}

	@Test
	void listPrivilegesReturnsAllPrivileges() {
		List<Privilege> privileges = List.of(
				new Privilege(PrivilegeRole.Owner),
				new Privilege(PrivilegeRole.Worker));
		when(privilegeRepository.findAll()).thenReturn(privileges);

		List<Privilege> result = privilegeService.listPrivileges();

		assertThat(result).containsExactlyElementsOf(privileges);
	}
}
