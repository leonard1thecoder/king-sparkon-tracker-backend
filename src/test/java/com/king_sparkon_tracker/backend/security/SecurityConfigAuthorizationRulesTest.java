package com.king_sparkon_tracker.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SecurityConfigAuthorizationRulesTest {

	@Test
	void positiveSubscribersArePublicAndPromotionsAreOwnerOnly() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/SecurityConfig.java"));

		assertThat(source).contains(".requestMatchers(HttpMethod.POST, \"/api/subscribers\").permitAll()");
		assertThat(source).contains(".requestMatchers(HttpMethod.DELETE, \"/api/subscribers\").permitAll()");
		assertThat(source).contains(".requestMatchers(\"/api/promotions/**\").hasAuthority(ownerAuthority)");
	}

	@Test
	void negativeSubscribersAreNotBehindGenericAuthenticationRuleOnly() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/SecurityConfig.java"));
		int subscriberRule = source.indexOf("/api/subscribers");
		int fallbackRule = source.indexOf(".anyRequest().authenticated()");

		assertThat(subscriberRule).isGreaterThanOrEqualTo(0);
		assertThat(fallbackRule).isGreaterThan(subscriberRule);
	}

	@Test
	void adminRegistrationIsProtectedByDefault() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/SecurityConfig.java"));
		String properties = Files.readString(Path.of("src/main/resources/application.properties"));

		assertThat(properties).contains("app.security.public-admin-registration-enabled=${PUBLIC_ADMIN_REGISTRATION_ENABLED:false}");
		assertThat(source).contains("@Value(\"${app.security.public-admin-registration-enabled:false}\") boolean publicAdminRegistrationEnabled");
		assertThat(source).contains("authorize.requestMatchers(HttpMethod.POST, REGISTER_ADMIN_PATH).hasAuthority(adminAuthority)");
	}

	@Test
	void userDashboardAndPublicTicketsHaveExplicitRules() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/SecurityConfig.java"));

		assertThat(source).contains(".requestMatchers(HttpMethod.GET, \"/api/v1/tickets/events\", \"/api/v1/tickets/events/**\").permitAll()");
		assertThat(source).contains(".requestMatchers(\"/api/user-dashboard\", \"/api/user-dashboard/**\").authenticated()");
		assertThat(source).contains(".requestMatchers(HttpMethod.POST, \"/api/v1/tickets/verify/qr\", \"/api/v1/tickets/verify/reference\").hasAuthority(workerAuthority)");
		assertThat(source).contains(".requestMatchers(\"/api/v1/tickets/owner/**\").hasAuthority(ownerAuthority)");
	}

	@Test
	void workerDashboardReadRoutesAreBeforeOwnerOnlyCatchalls() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/SecurityConfig.java"));

		String workerTransactionsRule = ".requestMatchers(HttpMethod.GET, \"/api/transactions/me\").hasAuthority(workerAuthority)";
		String ownerTransactionsRule = ".requestMatchers(HttpMethod.GET, \"/api/transactions\", \"/api/transactions/**\").hasAuthority(ownerAuthority)";
		String workerTipsRule = ".requestMatchers(HttpMethod.GET, \"/api/tips/me\").hasAuthority(workerAuthority)";
		String ownerTipsRule = ".requestMatchers(\"/api/tips\", \"/api/tips/**\").hasAuthority(ownerAuthority)";

		assertThat(source).contains(workerTransactionsRule);
		assertThat(source).contains(workerTipsRule);
		assertThat(source.indexOf(workerTransactionsRule)).isLessThan(source.indexOf(ownerTransactionsRule));
		assertThat(source.indexOf(workerTipsRule)).isLessThan(source.indexOf(ownerTipsRule));
	}

	@Test
	void businessAccessFilterOnlyRequiresBusinessForBusinessScopedRoles() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/BusinessAccessFilter.java"));

		assertThat(source).contains("BUSINESS_SCOPED_AUTHORITIES");
		assertThat(source).contains("PrivilegeRole.Owner.name()");
		assertThat(source).contains("PrivilegeRole.Worker.name()");
		assertThat(source).contains("PrivilegeRole.Affiliate.name()");
		assertThat(source).doesNotContain("PrivilegeRole.User.name()");
		assertThat(source).contains("/api/stripe/webhooks");
	}

	@Test
	void rateLimitCoversAuthAndExcludesWebhooks() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/security/RateLimitingFilter.java"));
		String properties = Files.readString(Path.of("src/main/resources/application.properties"));

		assertThat(source).contains("/api/auth/register-admin");
		assertThat(source).contains("/api/stripe/webhooks");
		assertThat(source).contains("normalizedPath(request)");
		assertThat(properties).contains("app.rate-limit.backend=${RATE_LIMIT_BACKEND:memory}");
	}
}
