package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class ProductPageableFactoryTest {

	private final ProductPageableFactory factory = new ProductPageableFactory();

	@Test
	void createCapsPageSizeAndMapsNameSort() {
		Pageable pageable = factory.create(-10, 500, "name", "asc");

		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(100);
		Sort.Order order = pageable.getSort().getOrderFor("name");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void createFallsBackToIdDescForUnsafeSort() {
		Pageable pageable = factory.create(1, 20, "drop table products", "sideways");

		assertThat(pageable.getPageNumber()).isEqualTo(1);
		assertThat(pageable.getPageSize()).isEqualTo(20);
		Sort.Order order = pageable.getSort().getOrderFor("id");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}
}
