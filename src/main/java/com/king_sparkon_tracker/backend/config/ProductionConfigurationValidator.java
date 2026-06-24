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
			"stripe.secret-key",
			"stripe.webhook-secret",
			"stripe.success-url",
			"stripe.cancel-url",
			"paypal.client-id",
			"paypal.client-secret",
			"paypal.webhook-id",
			"paypal.return-url",
			"paypal.cancel-url"
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

		List<String> missing = REQUIRED_PRODUCTION_PROPERTIES.stream()
				.filter(property -> !StringUtils.hasText(environment.getProperty(property)))
				.toList();

		if (!missing.isEmpty()) {
			throw new IllegalStateException("Production configuration is missing required properties: " + String.join(", ", missing));
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
