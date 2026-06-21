package com.king_sparkon_tracker.backend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Publishes business-facing OpenAPI metadata and the JWT security scheme used by Swagger UI.
 */
@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "King Sparkon Tracker API",
				version = "1.0.0",
				description = "Backend API for business-owned barcoded product inventory, worker transactions, reports, and audit logs.",
				contact = @Contact(name = "King Sparkon Tracker"),
				license = @License(name = "Private Business API")),
		servers = @Server(url = "/", description = "Current deployment"))
@SecurityScheme(
		name = OpenApiConfig.BEARER_AUTH,
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT")
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearer-jwt";

	/**
	 * Keeps the class available as a Spring bean so springdoc can discover the annotations at startup.
	 */
	public OpenApiConfig() {
	}
}
