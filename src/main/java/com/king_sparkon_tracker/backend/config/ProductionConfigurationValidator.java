package com.king_sparkon_tracker.backend.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionConfigurationValidator implements ApplicationRunner {

	private static final List<String> REQUIRED_PRODUCTION_PROPERTIES = List.of(
			"app.jwt.secret",
			"spring.datasource.url",
			"spring.datasource.username",
			"spring.datasource.password",
			"app.cors.allowed-origins",
			"app.frontend.reset-password-url",
			"app.frontend.email-verification-url",
			"app.frontend.login-url",
			"stripe.secret-key",
			"stripe.webhook-secret",
			"stripe.success-url",
			"stripe.cancel-url",
			"paypal.client-id",
			"paypal.client-secret",
			"paypal.webhook-id",
			"paypal.return-url",
			"paypal.cancel-url",
			"app.tips.paypal-onboarding-url",
			"app.tips.worker-tip-url-template",
			"app.transactions.paypal-onboarding-url"
	);

	private static final List<String> REQUIRED_CLOUD_RUN_ENV_VARS = List.of(
			"SUPABASE_DB_URL",
			"SUPABASE_DB_USER",
			"SUPABASE_DB_PASSWORD",
			"JWT_SECRET",
			"CORS_ALLOWED_ORIGINS",
			"FRONTEND_RESET_PASSWORD_URL",
			"FRONTEND_EMAIL_VERIFICATION_URL",
			"FRONTEND_LOGIN_URL",
			"STRIPE_SECRET_KEY",
			"STRIPE_WEBHOOK_SECRET",
			"STRIPE_SUCCESS_URL",
			"STRIPE_CANCEL_URL",
			"PAYPAL_CLIENT_ID",
			"PAYPAL_CLIENT_SECRET",
			"PAYPAL_WEBHOOK_ID",
			"PAYPAL_RETURN_URL",
			"PAYPAL_CANCEL_URL",
			"TIPS_PAYPAL_ONBOARDING_URL",
			"TIPS_WORKER_TIP_URL_TEMPLATE",
			"TRANSACTIONS_PAYPAL_ONBOARDING_URL",
			"RATE_LIMIT_BACKEND",
			"SPRING_DATA_REDIS_HOST"
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

		String rateLimitBackend = System.getenv("RATE_LIMIT_BACKEND");
		if (!"redis".equalsIgnoreCase(rateLimitBackend)) {
			throw new IllegalStateException("Production rate limiting must use RATE_LIMIT_BACKEND=redis");
		}

		String jwtSecret = environment.getProperty("app.jwt.secret", "");
		if (jwtSecret.length() < 64 || jwtSecret.contains("dev-only") || jwtSecret.contains("change-before")) {
			throw new IllegalStateException("Production JWT signing key must be at least 64 characters and must not use a development value");
		}

		requireNoLocalhost("stripe.success-url");
		requireNoLocalhost("stripe.cancel-url");
		requireNoLocalhost("paypal.return-url");
		requireNoLocalhost("paypal.cancel-url");
		requireNoLocalhost("app.frontend.reset-password-url");
		requireNoLocalhost("app.frontend.email-verification-url");
		requireNoLocalhost("app.frontend.login-url");
		requireNoLocalhost("app.tips.paypal-onboarding-url");
		requireNoLocalhost("app.tips.worker-tip-url-template");
		requireNoLocalhost("app.transactions.paypal-onboarding-url");
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
