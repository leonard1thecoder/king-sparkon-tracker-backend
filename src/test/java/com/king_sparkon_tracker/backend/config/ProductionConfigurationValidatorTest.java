package com.king_sparkon_tracker.backend.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {

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
}
