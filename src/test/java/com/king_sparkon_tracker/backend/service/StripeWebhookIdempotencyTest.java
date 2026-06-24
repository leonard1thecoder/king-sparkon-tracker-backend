package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;

import jakarta.persistence.Table;

class StripeWebhookIdempotencyTest {

	@Test
	void stripeWebhookEventHasUniqueEventIdConstraint() {
		Table table = StripeWebhookEvent.class.getAnnotation(Table.class);

		assertThat(table).isNotNull();
		assertThat(table.uniqueConstraints())
				.anySatisfy(uniqueConstraint -> assertThat(uniqueConstraint.columnNames()).containsExactly("stripe_event_id"));
	}
}
