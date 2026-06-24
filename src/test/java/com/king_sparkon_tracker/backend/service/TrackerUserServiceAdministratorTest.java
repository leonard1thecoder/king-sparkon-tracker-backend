package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.LoginRequest;
import com.king_sparkon_tracker.backend.dto.RegisterAdministratorRequest;
import com.king_sparkon_tracker.backend.exception.EmailNotVerifiedException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class TrackerUserServiceAdministratorTest {

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

	@Mock
	private AppEmailService appEmailService;

	private TrackerUserService userService;

	@BeforeEach
	void setUp() {
		userService = new TrackerUserService(
				userRepository,
				businessRepository,
				privilegeService,
				passwordEncoder,
				auditLogService,
				emailVerificationService,
				businessPlanPolicyService,
				businessAccessService,
				appEmailService,
				"https://app.example/tips/workers/{workerId}",
				"https://app.example/pricing?affiliateCode={affiliateCode}");
	}

	@Test
	void registerAdministratorCreatesSingleAdminWithKingsparkonDomainAndOnboarding() {
		Privilege adminPrivilege = new Privilege(PrivilegeRole.Admin);
		when(userRepository.countByPrivilege_Name(PrivilegeRole.Admin)).thenReturn(0L);
		when(userRepository.existsByUsername("sysadmin")).thenReturn(false);
		when(userRepository.existsByEmailAddress("admin@kingsparkon.com")).thenReturn(false);
		when(privilegeService.createPrivilege(PrivilegeRole.Admin)).thenReturn(adminPrivilege);
		when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
		when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> {
			TrackerUser user = invocation.getArgument(0);
			if (user.getId() == null) {
				ReflectionTestUtils.setField(user, "id", 99L);
			}
			return user;
		});

		TrackerUser result = userService.registerAdministrator(new RegisterAdministratorRequest(
				" sysadmin ",
				"ADMIN@KINGSPARKON.COM ",
				"secret",
				null,
				" 1 Admin Road ",
				" +27825550000 "));

		assertThat(result.getUsername()).isEqualTo("sysadmin");
		assertThat(result.getEmailAddress()).isEqualTo("admin@kingsparkon.com");
		assertThat(result.getPassword()).isEqualTo("encoded-secret");
		assertThat(result.getPrivilege()).isSameAs(adminPrivilege);
		assertThat(result.getBusiness()).isNull();
		assertThat(result.getPhysicalAddress()).isEqualTo("1 Admin Road");
		assertThat(result.getCellphoneNumber()).isEqualTo("+27825550000");
		assertThat(result.isOnboardingCompleted()).isTrue();
		assertThat(result.isOnboardingRequired()).isFalse();

		verify(auditLogService).record(
				eq("ADMINISTRATOR_REGISTERED"),
				eq("TrackerUser"),
				eq("99"),
				eq("sysadmin"),
				eq("Administrator registered with kingsparkon.com domain, localizationCountry: SOUTH_AFRICA"),
				eq(null));
		verify(emailVerificationService).sendVerificationEmail(result, null, null);
	}

	@Test
	void registerAdministratorRejectsNonKingsparkonEmailDomain() {
		assertThatThrownBy(() -> userService.registerAdministrator(new RegisterAdministratorRequest(
				"sysadmin",
				"admin@gmail.com",
				"secret",
				null,
				"1 Admin Road",
				"+27825550000")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Administrator email must use the @kingsparkon.com domain");

		verify(userRepository, never()).save(any(TrackerUser.class));
	}

	@Test
	void registerAdministratorRejectsSecondAdministrator() {
		when(userRepository.countByPrivilege_Name(PrivilegeRole.Admin)).thenReturn(1L);

		assertThatThrownBy(() -> userService.registerAdministrator(new RegisterAdministratorRequest(
				"sysadmin2",
				"admin2@kingsparkon.com",
				"secret",
				null,
				"1 Admin Road",
				"+27825550000")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Administrator account is already registered");

		verify(privilegeService, never()).createPrivilege(PrivilegeRole.Admin);
		verify(userRepository, never()).save(any(TrackerUser.class));
	}

	@Test
	void authenticateRejectsAdminWhenEmailIsNotVerified() {
		TrackerUser administrator = new TrackerUser(
				"sysadmin",
				"admin@kingsparkon.com",
				"encoded-secret",
				new Privilege(PrivilegeRole.Admin));
		when(userRepository.findByUsername("sysadmin")).thenReturn(Optional.of(administrator));
		when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

		assertThatThrownBy(() -> userService.authenticate(new LoginRequest("sysadmin", "secret")))
				.isInstanceOf(EmailNotVerifiedException.class)
				.hasMessage("Email address is not verified. Please verify your email before logging in.");
	}

	@Test
	void authenticateAllowsAdminWhenEmailIsVerified() {
		TrackerUser administrator = new TrackerUser(
				"sysadmin",
				"admin@kingsparkon.com",
				"encoded-secret",
				new Privilege(PrivilegeRole.Admin));
		administrator.markEmailVerified();
		when(userRepository.findByUsername("sysadmin")).thenReturn(Optional.of(administrator));
		when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

		TrackerUser result = userService.authenticate(new LoginRequest("sysadmin", "secret"));

		assertThat(result).isSameAs(administrator);
	}
}
