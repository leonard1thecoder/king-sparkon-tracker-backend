package com.king_sparkon_tracker.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

	@Test
	void healthReturnsLivenessWithoutDatabaseCheck() {
		HealthController controller = new HealthController("backend", mock(DataSource.class));

		HealthController.HealthResponse response = controller.health();

		assertThat(response.status()).isEqualTo("UP");
		assertThat(response.service()).isEqualTo("backend");
		assertThat(response.timestamp()).isNotNull();
	}

	@Test
	void readyReturnsOkWhenDatabaseResponds() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		when(statement.execute("SELECT 1")).thenReturn(true);
		HealthController controller = new HealthController("backend", dataSource);

		ResponseEntity<HealthController.ReadinessResponse> response = controller.ready();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo("UP");
		assertThat(response.getBody().checks().get("database").status()).isEqualTo("UP");
	}

	@Test
	void readyReturnsServiceUnavailableWhenDatabaseFails() throws Exception {
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));
		HealthController controller = new HealthController("backend", dataSource);

		ResponseEntity<HealthController.ReadinessResponse> response = controller.ready();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo("DOWN");
		assertThat(response.getBody().checks().get("database").message()).isEqualTo("database unavailable");
	}
}
