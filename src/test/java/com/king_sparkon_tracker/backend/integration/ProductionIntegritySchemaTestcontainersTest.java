package com.king_sparkon_tracker.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ProductionIntegritySchemaTestcontainersTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
			.withDatabaseName("king_sparkon_integrity")
			.withUsername("sparkon")
			.withPassword("sparkon-test");

	@BeforeAll
	static void migrate() {
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.locations("classpath:db/migration")
				.load()
				.migrate();
	}

	@Test
	void productionIntegrityTablesAndTenantColumnsExistOnPostgresql() throws Exception {
		assertThat(tableExists("api_idempotency_records")).isTrue();
		assertThat(tableExists("financial_journals")).isTrue();
		assertThat(tableExists("financial_ledger_lines")).isTrue();
		assertThat(tableExists("stock_reservations")).isTrue();
		assertThat(tableExists("ticket_reservations")).isTrue();
		assertThat(tableExists("outbox_events")).isTrue();
		assertThat(tableExists("generated_qr_assets")).isTrue();
		assertThat(tableExists("generated_reports")).isTrue();

		for (String table : List.of(
				"products",
				"inventory_transactions",
				"business_account_ledger_entries",
				"financial_journals",
				"stock_reservations",
				"ticket_events",
				"ticket_payments",
				"ticket_reservations",
				"user_tickets",
				"generated_reports")) {
			assertThat(columnExists(table, "business_id"))
					.as("%s must retain an explicit tenant key", table)
					.isTrue();
		}
	}

	@Test
	void idempotencyAndScarceResourceConstraintsRejectDuplicateOwnershipKeys() throws Exception {
		assertThat(uniqueConstraintExists("uk_api_idempotency_scope_actor_key")).isTrue();
		assertThat(uniqueConstraintExists("uk_financial_journal_source")).isTrue();
		assertThat(uniqueConstraintExists("uk_stock_reservation_payment_product")).isTrue();
		assertThat(uniqueConstraintExists("uk_ticket_reservation_payment")).isTrue();
		assertThat(indexExists("uk_user_tickets_qr_value")).isTrue();
	}

	@Test
	void tenantStatusReferenceBarcodeDateAndProviderIndexesExist() throws Exception {
		for (String index : List.of(
				"idx_api_idempotency_status_expiry",
				"idx_api_idempotency_business_created",
				"idx_financial_journal_business_posted",
				"idx_stock_reservation_expiry",
				"idx_stock_reservation_business_status",
				"idx_ticket_reservation_expiry",
				"idx_ticket_reservation_event_type",
				"idx_outbox_dispatch",
				"idx_products_business_status_stock",
				"idx_product_barcodes_product_availability",
				"idx_inventory_transactions_business_payment_date",
				"idx_inventory_transactions_payment_reference",
				"idx_tips_worker_status_created",
				"idx_business_account_ledger_provider_lookup",
				"idx_paypal_webhook_event_status_created",
				"idx_stripe_webhook_event_status_created")) {
			assertThat(indexExists(index)).as("missing production lookup index %s", index).isTrue();
		}
	}

	private static boolean tableExists(String table) throws Exception {
		return exists("select 1 from information_schema.tables where table_schema = 'public' and table_name = ?", table);
	}

	private static boolean columnExists(String table, String column) throws Exception {
		try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
				"select 1 from information_schema.columns where table_schema = 'public' and table_name = ? and column_name = ?")) {
			statement.setString(1, table);
			statement.setString(2, column);
			try (ResultSet result = statement.executeQuery()) {
				return result.next();
			}
		}
	}

	private static boolean indexExists(String index) throws Exception {
		return exists("select 1 from pg_indexes where schemaname = 'public' and indexname = ?", index);
	}

	private static boolean uniqueConstraintExists(String constraint) throws Exception {
		return exists("select 1 from information_schema.table_constraints where constraint_schema = 'public' and constraint_type = 'UNIQUE' and constraint_name = ?", constraint);
	}

	private static boolean exists(String sql, String value) throws Exception {
		try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, value);
			try (ResultSet result = statement.executeQuery()) {
				return result.next();
			}
		}
	}

	private static Connection connection() throws Exception {
		return java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
	}
}
