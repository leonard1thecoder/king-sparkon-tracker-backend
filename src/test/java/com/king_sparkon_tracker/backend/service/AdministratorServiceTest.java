package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.AdminOverviewResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class AdministratorServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	@Mock
	private BusinessRepository businessRepository;

	private AdministratorService administratorService;

	@BeforeEach
	void setUp() {
		administratorService = new AdministratorService(userRepository, businessRepository);
	}

	@Test
	void overviewReturnsPlatformTotalsForAdmin() {
		TrackerUser admin = user("admin", PrivilegeRole.Admin);
		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
		when(userRepository.count()).thenReturn(12L);
		when(userRepository.countByPrivilege_Name(PrivilegeRole.Admin)).thenReturn(1L);
		when(userRepository.countByPrivilege_Name(PrivilegeRole.Owner)).thenReturn(3L);
		when(userRepository.countByPrivilege_Name(PrivilegeRole.Worker)).thenReturn(7L);
		when(userRepository.countByPrivilege_Name(PrivilegeRole.Affiliate)).thenReturn(1L);
		when(businessRepository.count()).thenReturn(3L);

		AdminOverviewResponse result = administratorService.overview("admin");

		assertThat(result.totalUsers()).isEqualTo(12L);
		assertThat(result.totalAdministrators()).isEqualTo(1L);
		assertThat(result.totalOwners()).isEqualTo(3L);
		assertThat(result.totalWorkers()).isEqualTo(7L);
		assertThat(result.totalAffiliates()).isEqualTo(1L);
		assertThat(result.totalBusinesses()).isEqualTo(3L);
	}

	@Test
	void listUsersReturnsAllUsersForAdmin() {
		TrackerUser admin = user("admin", PrivilegeRole.Admin);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		PageRequest pageRequest = PageRequest.of(0, 20);
		Page<TrackerUser> users = new PageImpl<>(List.of(admin, owner), pageRequest, 2);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
		when(userRepository.findAll(pageRequest)).thenReturn(users);

		Page<TrackerUser> result = administratorService.listUsers(pageRequest, "admin");

		assertThat(result.getContent()).containsExactly(admin, owner);
	}

	@Test
	void listBusinessesReturnsAllBusinessesForAdmin() {
		TrackerUser admin = user("admin", PrivilegeRole.Admin);
		Business business = business();
		PageRequest pageRequest = PageRequest.of(0, 20);
		Page<Business> businesses = new PageImpl<>(List.of(business), pageRequest, 1);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
		when(businessRepository.findAll(pageRequest)).thenReturn(businesses);

		Page<Business> result = administratorService.listBusinesses(pageRequest, "admin");

		assertThat(result.getContent()).containsExactly(business);
	}

	@Test
	void adminAccessRejectsOwner() {
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

		assertThatThrownBy(() -> administratorService.overview("owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Only administrators can access platform administration");
	}

	@Test
	void getBusinessByIdRejectsMissingBusiness() {
		TrackerUser admin = user("admin", PrivilegeRole.Admin);
		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
		when(businessRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> administratorService.getBusinessById(99L, "admin"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Business not found: 99");
	}

	private TrackerUser user(String username, PrivilegeRole role) {
		TrackerUser user = new TrackerUser(username, username + "@example.com", "encoded", new Privilege(role));
		ReflectionTestUtils.setField(user, "id", role.ordinal() + 1L);
		return user;
	}

	private Business business() {
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 7L);
		owner.setBusiness(business);
		return business;
	}
}
