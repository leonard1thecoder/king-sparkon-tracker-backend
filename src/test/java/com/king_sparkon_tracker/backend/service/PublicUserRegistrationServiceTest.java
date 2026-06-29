package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.dto.ServiceRegistrationFor;
import com.king_sparkon_tracker.backend.exception.DuplicateEmailAddressException;
import com.king_sparkon_tracker.backend.exception.DuplicateUsernameException;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PublicUserRegistrationServiceTest {

    @Mock
    private TrackerUserRepository userRepository;

    @Mock
    private PrivilegeService privilegeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailVerificationService emailVerificationService;

    private PublicUserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new PublicUserRegistrationService(
                userRepository,
                privilegeService,
                passwordEncoder,
                auditLogService,
                emailVerificationService);
    }

    @Test
    void registerUserCreatesUserPrivilegeAndSendsVerificationEmail() {
        Privilege userPrivilege = new Privilege(PrivilegeRole.User);
        when(userRepository.existsByUsername("client")).thenReturn(false);
        when(userRepository.existsByEmailAddress("client@example.com")).thenReturn(false);
        when(privilegeService.createPrivilege(PrivilegeRole.User)).thenReturn(userPrivilege);
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("encoded");
        when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrackerUser result = service.registerUser(new RegisterUserRequest(
                " client ",
                " CLIENT@EXAMPLE.COM ",
                "StrongPassword123",
                null,
                null,
                LocalizationCountry.SOUTH_AFRICA,
                " 12 Client Road ",
                " +27821234567 ",
                null,
                ServiceRegistrationFor.USER,
                null));

        assertThat(result.getUsername()).isEqualTo("client");
        assertThat(result.getEmailAddress()).isEqualTo("client@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getPrivilege()).isSameAs(userPrivilege);
        assertThat(result.getBusiness()).isNull();
        assertThat(result.getPhysicalAddress()).isEqualTo("12 Client Road");
        assertThat(result.getCellphoneNumber()).isEqualTo("+27821234567");
        assertThat(result.isOnboardingCompleted()).isTrue();
        verify(auditLogService).record(eq("USER_REGISTERED"), eq("TrackerUser"), any(), eq("client"), any(), eq(null));
        verify(emailVerificationService).sendVerificationEmail(result, null, null);
    }

    @Test
    void registerUserRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("client")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(new RegisterUserRequest(
                "client",
                "client@example.com",
                "StrongPassword123",
                null,
                LocalizationCountry.SOUTH_AFRICA)))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessage("Username already exists: client");
    }

    @Test
    void registerUserRejectsDuplicateEmailAddress() {
        when(userRepository.existsByUsername("client")).thenReturn(false);
        when(userRepository.existsByEmailAddress("client@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(new RegisterUserRequest(
                "client",
                "client@example.com",
                "StrongPassword123",
                null,
                LocalizationCountry.SOUTH_AFRICA)))
                .isInstanceOf(DuplicateEmailAddressException.class)
                .hasMessage("Email address already exists: client@example.com");
    }
}
