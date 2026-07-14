package com.king_sparkon_tracker.backend.tickets.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;

class EventTicketTypeReservationTest {

	@Test
	void reservationsCannotOversellAndNeverMakeCapacityNegative() {
		EventTicketType type = new EventTicketType();
		type.setCapacity(3);
		type.setSold(0);
		type.setReserved(0);
		type.setAvailable(3);

		type.reserve(2);
		assertThat(type.getReserved()).isEqualTo(2);
		assertThat(type.getAvailable()).isEqualTo(1);

		assertThatThrownBy(() -> type.reserve(2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("insufficient");
		assertThat(type.getAvailable()).isEqualTo(1);
		assertThat(type.getSold()).isZero();

		type.consumeReservation(2);
		assertThat(type.getReserved()).isZero();
		assertThat(type.getSold()).isEqualTo(2);
		assertThat(type.getAvailable()).isEqualTo(1);
	}

	@Test
	void expiredReservationRestoresOnlyReservedCapacity() {
		EventTicketType type = new EventTicketType();
		type.setCapacity(10);
		type.setSold(4);
		type.setReserved(0);
		type.setAvailable(6);

		type.reserve(3);
		type.releaseReservation(3);

		assertThat(type.getSold()).isEqualTo(4);
		assertThat(type.getReserved()).isZero();
		assertThat(type.getAvailable()).isEqualTo(6);
	}
}
