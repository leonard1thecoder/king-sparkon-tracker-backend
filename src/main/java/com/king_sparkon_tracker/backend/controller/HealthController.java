package com.king_sparkon_tracker.backend.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health", description = "Runtime health checks.")
public class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final String serviceName;
	private final DataSource dataSource;

	public HealthController(
			@Value("${spring.application.name:backend}") String serviceName,
			DataSource dataSource) {
		this.serviceName = serviceName;
		this.dataSource = dataSource;
	}

	@GetMapping({ "/health", "/api/health" })
	@Operation(summary = "Health check", description = "Public liveness check for hosting platforms and frontend startup checks.")
	public HealthResponse health() {
		return new HealthResponse("UP", serviceName, Instant.now());
	}

	@GetMapping({ "/ready", "/api/ready" })
	@Operation(summary = "Readiness check", description = "Public readiness check that verifies database connectivity.")
	public ResponseEntity<ReadinessResponse> ready() {
		DatabaseCheck database = databaseCheck();
		String status = database.status();
		HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus)
				.body(new ReadinessResponse(status, serviceName, Instant.now(), Map.of("database", database)));
	}

	private DatabaseCheck databaseCheck() {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			statement.setQueryTimeout(2);
			statement.execute("SELECT 1");
			return new DatabaseCheck("UP", null);
		} catch (SQLException exception) {
			log.warn("readiness_database_check_failed message={}", exception.getMessage());
			return new DatabaseCheck("DOWN", exception.getMessage());
		}
	}

	public record HealthResponse(String status, String service, Instant timestamp) {
	}

	public record ReadinessResponse(
			String status,
			String service,
			Instant timestamp,
			Map<String, DatabaseCheck> checks) {
	}

	public record DatabaseCheck(String status, String message) {
	}
}
