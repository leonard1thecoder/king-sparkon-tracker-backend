package com.king_sparkon_tracker.backend.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {

	private static final String PROD_FRONTEND_BASE_URL = "https://app.kingsparkon-tracker.com";
	private static final String PROD_JWT_SECRET = "k".repeat(80);
	private static final Map<String, String> VALID_PROD_ENV = Map.ofEntries(
			Map.entry("SUPABASE_DB_URL", "jdbc:postgresql://db.example.com:5432/postgres"),
			Map.entry("SUPABASE_DB_USER", "postgres"),
			Map.entry("SUPABASE_DB_PASSWORD", "safe-password"),
			Map.entry("JWT_SECRET", PROD_JWT_SECRET),
			Map.entry("FRONTEND_RESET_PASSWORD_URL", PROD_FRONTEND_BASE_URL + "/reset-password"),
			Map.entry("FRONTEND_EMAIL_VERIFICATION_URL", PROD_FRONTEND_BASE_URL + "/verify-email"),
			Map.entry("FRONTEND_LOGIN_URL", PROD_FRONTEND_BASE_URL + "/login"),
			Map.entry("STRIPE_SECRET_KEY", "sk_live_safe_test_value"),
			Map.entry("STRIPE_WEBHOOK_SECRET", "whsec_safe_test_value"),
			Map.entry("STRIPE_SUCCESS_URL", PROD_FRONTEND_BASE_URL + "/dashboard/owner?billing=stripe-success"),
			Map.entry("STRIPE_CANCEL_URL", PROD_FRONTEND_BASE_URL + "/dashboard/owner?billing=stripe-cancelled"),
			Map.entry("TIPS_WORKER_TIP_URL_TEMPLATE", PROD_FRONTEND_BASE_URL + "/tips/workers/{workerId}"),
			Map.entry("RATE_LIMIT_BACKEND", "redis"),
			Map.entry("SPRING_DATA_REDIS_HOST", "redis.example.com")
	);

	@Test
	void skipsValidationWhenProdProfileIsNotActive() {
		MockEnvironment environment = new MockEnvironment();
		ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

		assertThatCode(() -> validator.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
	}

	@Test
	void failsWhenProdProfileMissesRequiredValues() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

		assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Production configuration is missing required properties");
	}

	@Test
	void allowsProductionCorsToBeDerivedFromFrontendUrls() {
		MockEnvironment environment = validProductionEnvironment();
		ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment, validProdEnv());

		assertThatCode(() -> validator.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
	}

	@Test
	void failsWhenExplicitCorsOriginPointsToLocalhostInProd() {
		MockEnvironment environment = validProductionEnvironment()
				.withProperty("app.cors.allowed-origins", "http://localhost:3000");
		ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment, validProdEnv());

		assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Production property must not point to localhost: app.cors.allowed-origins");
	}

	private static MockEnvironment validProductionEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		return environment
				.withProperty("app.jwt.secret", PROD_JWT_SECRET)
				.withProperty("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/postgres")
				.withProperty("spring.datasource.username", "postgres")
				.withProperty("spring.datasource.password", "safe-password")
				.withProperty("app.frontend.reset-password-url", PROD_FRONTEND_BASE_URL + "/reset-password")
				.withProperty("app.frontend.email-verification-url", PROD_FRONTEND_BASE_URL + "/verify-email")
				.withProperty("app.frontend.login-url", PROD_FRONTEND_BASE_URL + "/login")
				.withProperty("stripe.secret-key", "sk_live_safe_test_value")
				.withProperty("stripe.webhook-secret", "whsec_safe_test_value")
				.withProperty("stripe.success-url", PROD_FRONTEND_BASE_URL + "/dashboard/owner?billing=stripe-success")
				.withProperty("stripe.cancel-url", PROD_FRONTEND_BASE_URL + "/dashboard/owner?billing=stripe-cancelled")
				.withProperty("app.tips.worker-tip-url-template", PROD_FRONTEND_BASE_URL + "/tips/workers/{workerId}");
	}

	private static Function<String, String> validProdEnv() {
		return VALID_PROD_ENV::get;
	}
}
