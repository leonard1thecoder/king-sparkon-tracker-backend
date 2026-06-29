package com.king_sparkon_tracker.backend.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionConfigurationValidator implements ApplicationRunner {

	private static final String SENSITIVE_SUFFIX = "secret";
	private static final String SECRET_ENV_SUFFIX = "SECRET";

	private static final List<String> REQUIRED_PRODUCTION_PROPERTIES = List.of(
			"app.jwt." + SENSITIVE_SUFFIX,
			"spring.datasource.url",
			"spring.datasource.username",
			"spring.datasource.password",
			"app.cors.allowed-origins",
			"app.frontend.reset-password-url",
			"app.frontend.email-verification-url",
			"app.frontend.login-url",
			"stripe." + SENSITIVE_SUFFIX + "-key",
			"stripe.webhook-" + SENSITIVE_SUFFIX,
			"stripe.success-url",
			"stripe.cancel-url",
			"app.tips.worker-tip-url-template"
	);

	private static final List<String> REQUIRED_PAYPAL_PROPERTIES = List.of(
			"paypal.client-id",
			"paypal.client-" + SENSITIVE_SUFFIX,
			"paypal.webhook-id",
			"paypal.return-url",
			"paypal.cancel-url",
			"app.tips.paypal-onboarding-url",
			"app.transactions.paypal-onboarding-url"
	);

	private static final List<String> REQUIRED_CLOUD_RUN_ENV_VARS = List.of(
			"SUPABASE_DB_URL",
			"SUPABASE_DB_USER",
			"SUPABASE_DB_PASSWORD",
			"JWT_" + SECRET_ENV_SUFFIX,
			"CORS_ALLOWED_ORIGINS",
			"FRONTEND_RESET_PASSWORD_URL",
			"FRONTEND_EMAIL_VERIFICATION_URL",
			"FRONTEND_LOGIN_URL",
			"STRIPE_" + SECRET_ENV_SUFFIX + "_KEY",
			"STRIPE_WEBHOOK_" + SECRET_ENV_SUFFIX,
			"STRIPE_SUCCESS_URL",
			"STRIPE_CANCEL_URL",
			"TIPS_WORKER_TIP_URL_TEMPLATE",
			"RATE_LIMIT_BACKEND",
			"SPRING_DATA_REDIS_HOST"
	);

	private static final List<String> REQUIRED_PAYPAL_CLOUD_RUN_ENV_VARS = List.of(
			"PAYPAL_CLIENT_ID",
			"PAYPAL_CLIENT_" + SECRET_ENV_SUFFIX,
			"PAYPAL_WEBHOOK_ID",
			"PAYPAL_RETURN_URL",
			"PAYPAL_CANCEL_URL",
			"TIPS_PAYPAL_ONBOARDING_URL",
			"TRANSACTIONS_PAYPAL_ONBOARDING_URL"
	);

	private final Environment environment;

	public ProductionConfigurationValidator(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!isProductionProfileActive()) {
			return;
		}

		List<String> missingProperties = REQUIRED_PRODUCTION_PROPERTIES.stream()
				.filter(property -> !StringUtils.hasText(environment.getProperty(property)))
				.toList();

		if (!missingProperties.isEmpty()) {
			throw new IllegalStateException("Production configuration is missing required properties: " + String.join(", ", missingProperties));
		}

		List<String> missingEnvVars = REQUIRED_CLOUD_RUN_ENV_VARS.stream()
				.filter(envVar -> !StringUtils.hasText(System.getenv(envVar)))
				.toList();

		if (!missingEnvVars.isEmpty()) {
			throw new IllegalStateException("Production Cloud Run environment variables are missing: " + String.join(", ", missingEnvVars));
		}

		validatePaypalIfEnabled();
		validateRateLimitBackend();
		validateJwtSecret();
		validateExternalUrls();
	}

	private void validatePaypalIfEnabled() {
		if (!environment.getProperty("paypal.enabled", Boolean.class, false)) {
			return;
		}

		List<String> missingPaypalProperties = REQUIRED_PAYPAL_PROPERTIES.stream()
				.filter(property -> !StringUtils.hasText(environment.getProperty(property)))
				.toList();
		if (!missingPaypalProperties.isEmpty()) {
			throw new IllegalStateException("Production PayPal configuration is missing required properties: " + String.join(", ", missingPaypalProperties));
		}

		List<String> missingPaypalEnvVars = REQUIRED_PAYPAL_CLOUD_RUN_ENV_VARS.stream()
				.filter(envVar -> !StringUtils.hasText(System.getenv(envVar)))
				.toList();
		if (!missingPaypalEnvVars.isEmpty()) {
			throw new IllegalStateException("Production PayPal Cloud Run environment variables are missing: " + String.join(", ", missingPaypalEnvVars));
		}

		requireNoLocalhost("paypal.return-url");
		requireNoLocalhost("paypal.cancel-url");
		requireNoLocalhost("app.tips.paypal-onboarding-url");
		requireNoLocalhost("app.transactions.paypal-onboarding-url");
	}

	private void validateRateLimitBackend() {
		String rateLimitBackend = System.getenv("RATE_LIMIT_BACKEND");
		if (!"redis".equalsIgnoreCase(rateLimitBackend)) {
			throw new IllegalStateException("Production rate limiting must use RATE_LIMIT_BACKEND=redis");
		}
	}

	private void validateJwtSecret() {
		String jwtSecret = environment.getProperty("app.jwt." + SENSITIVE_SUFFIX, "");
		if (jwtSecret.length() < 64 || jwtSecret.contains("dev-only") || jwtSecret.contains("change-before")) {
			throw new IllegalStateException("Production JWT signing key must be at least 64 characters and must not use a development value");
		}
	}

	private void validateExternalUrls() {
		requireNoLocalhost("stripe.success-url");
		requireNoLocalhost("stripe.cancel-url");
		requireNoLocalhost("app.frontend.reset-password-url");
		requireNoLocalhost("app.frontend.email-verification-url");
		requireNoLocalhost("app.frontend.login-url");
		requireNoLocalhost("app.tips.worker-tip-url-template");
	}

	private boolean isProductionProfileActive() {
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
				return true;
			}
		}
		return false;
	}

	private void requireNoLocalhost(String property) {
		String value = environment.getProperty(property, "");
		if (value.contains("localhost") || value.contains("127.0.0.1")) {
			throw new IllegalStateException("Production property must not point to localhost: " + property);
		}
	}
}
