package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class DevHubPageableFactoryTest {

	private final DevHubPageableFactory factory = new DevHubPageableFactory();

	@Test
	void createCapsPageSizeAndMapsCreatedSort() {
		Pageable pageable = factory.create(-5, 500, "createdAt", "asc");

		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(100);
		Sort.Order order = pageable.getSort().getOrderFor("createdAt");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void createFallsBackToCreatedAtDescForUnsafeSort() {
		Pageable pageable = factory.create(2, 20, "drop table dev_hub", "sideways");

		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(20);
		Sort.Order order = pageable.getSort().getOrderFor("createdAt");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}
}
