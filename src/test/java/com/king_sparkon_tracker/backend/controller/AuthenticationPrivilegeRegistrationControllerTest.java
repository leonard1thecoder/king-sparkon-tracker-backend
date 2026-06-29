package com.king_sparkon_tracker.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.king_sparkon_tracker.backend.dto.RegisterAffiliateRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;
import com.king_sparkon_tracker.backend.service.PublicUserRegistrationService;
import com.king_sparkon_tracker.backend.service.RefreshTokenService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthenticationPrivilegeRegistrationControllerTest {

    private TrackerUserService userService;
    private PublicUserRegistrationService publicUserRegistrationService;
    private BusinessRepository businessRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(TrackerUserService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        publicUserRegistrationService = mock(PublicUserRegistrationService.class);
        businessRepository = mock(BusinessRepository.class);

        AuthenticationController controller = new AuthenticationController(
                userService,
                refreshTokenService,
                passwordResetService,
                emailVerificationService);
        ReflectionTestUtils.setField(controller, "publicUserRegistrationService", publicUserRegistrationService);
        ReflectionTestUtils.setField(controller, "businessRepository", businessRepository);
        ReflectionTestUtils.setField(controller, "businessUrlTemplate", "http://localhost:3000/businesses/{businessId}");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void registerRoutesUserSelectionToUserPrivilegeService() throws Exception {
        TrackerUser user = user("client", PrivilegeRole.User);
        when(publicUserRegistrationService.registerUser(any(RegisterUserRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceRegisteringFor": "USER",
                                  "username": "client",
                                  "emailAddress": "client@example.com",
                                  "password": "StrongPassword123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("client"))
                .andExpect(jsonPath("$.privilege").value("User"));

        verify(publicUserRegistrationService).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    void registerRoutesAffiliateSelectionToAffiliatePrivilegeService() throws Exception {
        TrackerUser affiliate = user("partner", PrivilegeRole.Affiliate);
        affiliate.activateAffiliateProfile("AFF-PARTNER-12345678", "http://localhost:3000/pricing", "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=AFF");
        when(userService.registerAffiliate(any(RegisterAffiliateRequest.class))).thenReturn(affiliate);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceRegisteringFor": "AFFLIATE",
                                  "username": "partner",
                                  "emailAddress": "partner@example.com",
                                  "password": "StrongPassword123",
                                  "paypalLink": "https://example.com/partner"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("partner"))
                .andExpect(jsonPath("$.privilege").value("Affiliate"))
                .andExpect(jsonPath("$.affiliateQrCodeUrl").exists());

        verify(userService).registerAffiliate(any(RegisterAffiliateRequest.class));
    }

    @Test
    void registerBusinessOwnerCreatesBusinessQrCode() throws Exception {
        TrackerUser owner = user("owner", PrivilegeRole.Owner);
        Business business = new Business("Owner Store", owner);
        ReflectionTestUtils.setField(business, "id", 77L);
        owner.setBusiness(business);
        when(userService.registerOwner(any(RegisterUserRequest.class))).thenReturn(owner);
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceRegisteringFor": "BUSINESS_OWNER",
                                  "username": "owner",
                                  "emailAddress": "owner@example.com",
                                  "password": "StrongPassword123",
                                  "businessName": "Owner Store"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.privilege").value("Owner"))
                .andExpect(jsonPath("$.businessQrCodeUrl").exists())
                .andExpect(jsonPath("$.businessQrCodeUrl").value(containsString("businesses%2F77")));

        verify(businessRepository).save(business);
    }

    private TrackerUser user(String username, PrivilegeRole role) {
        return new TrackerUser(username, username + "@example.com", "encoded", new Privilege(role));
    }
}
