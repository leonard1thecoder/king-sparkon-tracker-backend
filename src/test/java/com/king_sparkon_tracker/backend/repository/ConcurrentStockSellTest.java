package com.king_sparkon_tracker.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

class ConcurrentStockSellTest {

	@Test
	void stockUpdateQueriesUsePessimisticWriteLocks() throws Exception {
		Method byId = ProductRepository.class.getMethod("findLockedById", Long.class);
		Method byBusiness = ProductRepository.class.getMethod("findLockedByIdAndBusiness_Id", Long.class, Long.class);

		assertThat(byId.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(byBusiness.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}
}
