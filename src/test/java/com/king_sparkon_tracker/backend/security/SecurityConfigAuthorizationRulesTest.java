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
}
