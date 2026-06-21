package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.king_sparkon_tracker.backend.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.LoginRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;

@ExtendWith(MockitoExtension.class)
class TrackerUserServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	@Mock
	private BusinessRepository businessRepository;

	@Mock
	private PrivilegeService privilegeService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private EmailVerificationService emailVerificationService;

	@Spy
	private BusinessPlanPolicyService businessPlanPolicyService = new BusinessPlanPolicyService();

	@Mock
	private BusinessAccessService businessAccessService;

	@InjectMocks
	private TrackerUserService userService;

	@Test
	void registerOwnerCreatesOwnerAndSendsVerificationEmail() {
		Privilege ownerPrivilege = new Privilege(PrivilegeRole.Owner);
		when(userRepository.existsByUsername("alice")).thenReturn(false);
		when(userRepository.existsByEmailAddress("alice@example.com")).thenReturn(false);
		when(privilegeService.createPrivilege(PrivilegeRole.Owner)).thenReturn(ownerPrivilege);
		when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
		when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TrackerUser result = userService.registerOwner(new RegisterUserRequest("alice", "alice@example.com", "secret", "Alice Store", null));

		assertThat(result.isEmailVerified()).isFalse();
		verify(emailVerificationService).sendVerificationEmail(result, null, null);
	}

	@Test
	void authenticateRejectsOwnerWhenEmailIsNotVerified() {
		TrackerUser user = new TrackerUser("owner", "owner@example.com", "encoded-secret", new Privilege(PrivilegeRole.Owner));
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

		assertThatThrownBy(() -> userService.authenticate(new LoginRequest("owner", "secret")))
				.isInstanceOf(EmailNotVerifiedException.class)
				.hasMessage("Email address is not verified. Please verify your email before logging in.");
	}

    @Test
    void deleteWorkerDeletesWorkerInOwnersBusinessAndAudits() {
        Business business = business();

        TrackerUser worker = user("worker", "encoded", PrivilegeRole.Worker);
        ReflectionTestUtils.setField(worker, "id", 2L);
        worker.setBusiness(business);

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(business.getOwner()));
        when(userRepository.findByIdAndBusiness_Id(2L, 1L)).thenReturn(Optional.of(worker));

        userService.deleteWorker(2L, "owner");

        verify(auditLogService).record(
                eq("WORKER_DELETED"),
                eq("TrackerUser"),
                eq("2"),
                eq("owner"),
                eq("Worker deleted: worker"),
                eq(business));

        verify(userRepository).delete(worker);
    }

    @Test
    void deleteWorkerRejectsMissingWorker() {
        Business business = business();

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(business.getOwner()));
        when(userRepository.findByIdAndBusiness_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteWorker(99L, "owner"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Worker not found: 99");

        verify(userRepository, never()).delete(Mockito.any());
    }

    @Test
    void deleteWorkerRejectsOwnerSelfDelete() {
        Business business = business();
        TrackerUser owner = business.getOwner();
        ReflectionTestUtils.setField(owner, "id", 1L);

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(userRepository.findByIdAndBusiness_Id(1L, 1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> userService.deleteWorker(1L, "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner cannot be deleted through worker delete endpoint");

        verify(userRepository, never()).delete(Mockito.any());
    }

    @Test
    void deleteWorkerRejectsNonWorkerAccount() {
        Business business = business();

        TrackerUser anotherOwner = user("another-owner", "encoded", PrivilegeRole.Owner);
        ReflectionTestUtils.setField(anotherOwner, "id", 3L);
        anotherOwner.setBusiness(business);

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(business.getOwner()));
        when(userRepository.findByIdAndBusiness_Id(3L, 1L)).thenReturn(Optional.of(anotherOwner));

        assertThatThrownBy(() -> userService.deleteWorker(3L, "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only worker accounts can be deleted");

        verify(userRepository, never()).delete(Mockito.any());
    }

    @Test
    void deleteWorkerRejectsNullWorkerId() {
        assertThatThrownBy(() -> userService.deleteWorker(null, "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Worker id is required");

        verify(userRepository, never()).delete(Mockito.any());
    }

	@Test
	void authenticateAllowsOwnerWhenEmailIsVerified() {
		TrackerUser user = new TrackerUser("owner", "owner@example.com", "encoded-secret", new Privilege(PrivilegeRole.Owner));
		user.markEmailVerified();
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

		TrackerUser result = userService.authenticate(new LoginRequest("owner", "secret"));

		assertThat(result).isSameAs(user);
	}

	@Test
	void registerOwnerCreatesOwnerBusinessAndEncodesPassword() {
		Privilege ownerPrivilege = new Privilege(PrivilegeRole.Owner);
		when(userRepository.existsByUsername("alice")).thenReturn(false);
		when(userRepository.existsByEmailAddress("alice@example.com")).thenReturn(false);
		when(privilegeService.createPrivilege(PrivilegeRole.Owner)).thenReturn(ownerPrivilege);
		when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
		when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> {
			Business business = invocation.getArgument(0);
			ReflectionTestUtils.setField(business, "id", 1L);
			return business;
		});

		TrackerUser result = userService.registerOwner(
				new RegisterUserRequest(" alice ", "ALICE@EXAMPLE.COM ", "secret", " Alice Store ", null));

		assertThat(result.getUsername()).isEqualTo("alice");
		assertThat(result.getEmailAddress()).isEqualTo("alice@example.com");
		assertThat(result.getPassword()).isEqualTo("encoded-secret");
		assertThat(result.getPrivilege()).isSameAs(ownerPrivilege);
		assertThat(result.getBusiness().getName()).isEqualTo("Alice Store");
	}

	@Test
	void createWorkerCreatesWorkerPrivilege() {
		Privilege workerPrivilege = new Privilege(PrivilegeRole.Worker);
		Business business = business();
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(business.getOwner()));
		when(userRepository.countByBusiness_IdAndPrivilege_Name(1L, PrivilegeRole.Worker)).thenReturn(1L);
		when(userRepository.existsByUsername("worker")).thenReturn(false);
		when(userRepository.existsByEmailAddress("worker@example.com")).thenReturn(false);
		when(privilegeService.createPrivilege(PrivilegeRole.Worker)).thenReturn(workerPrivilege);
		when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
		when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TrackerUser result = userService.createWorker(
				new CreateWorkerRequest("worker", "worker@example.com", "secret"), "owner");

		assertThat(result.getUsername()).isEqualTo("worker");
		assertThat(result.getEmailAddress()).isEqualTo("worker@example.com");
		assertThat(result.getPrivilege()).isSameAs(workerPrivilege);
		assertThat(result.getBusiness()).isSameAs(business);
	}

	@Test
	void createWorkerRejectsMoreThanTwoWorkers() {
		Business business = business();
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(business.getOwner()));
		when(userRepository.countByBusiness_IdAndPrivilege_Name(1L, PrivilegeRole.Worker)).thenReturn(2L);

		assertThatThrownBy(() -> userService.createWorker(
				new CreateWorkerRequest("worker3", "worker3@example.com", "secret"), "owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Worker limit reached for FREE_TRIAL plan. Current limit: 2");
	}

	@Test
	void registerOwnerRejectsDuplicateUsername() {
		when(userRepository.existsByUsername("alice")).thenReturn(true);

		assertThatThrownBy(() -> userService.registerOwner(
				new RegisterUserRequest("alice", "alice@example.com", "secret", "Alice Store", null)))
				.isInstanceOf(DuplicateUsernameException.class)
				.hasMessage("Username already exists: alice");
	}

	@Test
	void registerOwnerRejectsDuplicateEmailAddress() {
		when(userRepository.existsByUsername("alice")).thenReturn(false);
		when(userRepository.existsByEmailAddress("alice@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.registerOwner(
				new RegisterUserRequest("alice", "alice@example.com", "secret", "Alice Store", null)))
				.isInstanceOf(DuplicateEmailAddressException.class)
				.hasMessage("Email address already exists: alice@example.com");
	}

	@Test
	void registerOwnerRejectsBlankBusinessName() {
		assertThatThrownBy(() -> userService.registerOwner(
				new RegisterUserRequest("alice", "alice@example.com", "secret", " ", null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Business name is required");
	}

	@Test
	void registerOwnerRejectsBlankUsername() {
		assertThatThrownBy(() -> userService.registerOwner(
				new RegisterUserRequest(" ", "alice@example.com", "secret", "Alice Store", null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Username is required");
	}

	@Test
	void registerOwnerRejectsBlankEmailAddress() {
		assertThatThrownBy(() -> userService.registerOwner(new RegisterUserRequest("alice", " ", "secret", "Alice Store", null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email address is required");
	}

	@Test
	void registerOwnerRejectsBlankPassword() {
		assertThatThrownBy(() -> userService.registerOwner(
				new RegisterUserRequest("alice", "alice@example.com", " ", "Alice Store", null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Password is required");
	}

	@Test
	void authenticateReturnsUserForValidCredentials() {
		TrackerUser user = user("alice", "encoded-secret", PrivilegeRole.Worker);
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

		TrackerUser result = userService.authenticate(new LoginRequest(" alice ", "secret"));

		assertThat(result).isSameAs(user);
	}

	@Test
	void authenticateThrowsForUnknownUsername() {
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.authenticate(new LoginRequest("missing", "secret")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid username or password");
	}

	@Test
	void authenticateThrowsForBadPassword() {
		TrackerUser user = user("alice", "encoded-secret", PrivilegeRole.Worker);
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "encoded-secret")).thenReturn(false);

		assertThatThrownBy(() -> userService.authenticate(new LoginRequest("alice", "wrong")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid username or password");
	}

	@Test
	void listUsersReturnsAllUsers() {
		List<TrackerUser> users = List.of(
				user("owner", "encoded", PrivilegeRole.Owner),
				user("worker", "encoded", PrivilegeRole.Worker));
		when(userRepository.findAll()).thenReturn(users);

		List<TrackerUser> result = userService.listUsers();

		assertThat(result).containsExactlyElementsOf(users);
	}

	@Test
	void getUserByIdReturnsUser() {
		TrackerUser user = user("alice", "encoded", PrivilegeRole.Worker);
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));

		TrackerUser result = userService.getUserById(7L);

		assertThat(result).isSameAs(user);
	}

	@Test
	void getUserByIdThrowsWhenMissing() {
		when(userRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUserById(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found: 7");
	}

	@Test
	void getUserByUsernameTrimsAndReturnsUser() {
		TrackerUser user = user("alice", "encoded", PrivilegeRole.Worker);
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

		TrackerUser result = userService.getUserByUsername(" alice ");

		assertThat(result).isSameAs(user);
	}

	@Test
	void getUserByUsernameThrowsWhenMissing() {
		when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUserByUsername("alice"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found: alice");
	}

	private TrackerUser user(String username, String password, PrivilegeRole role) {
		return new TrackerUser(username, username + "@example.com", password, new Privilege(role));
	}

	private Business business() {
		TrackerUser owner = user("owner", "encoded", PrivilegeRole.Owner);
		ReflectionTestUtils.setField(owner, "id", 1L);

		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);

		owner.setBusiness(business);
		return business;
	}
}
