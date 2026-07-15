package com.king_sparkon_tracker.backend.outbox;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxDispatchService {

	private final OutboxEventRepository repository;
	private final Map<OutboxEventType, OutboxEventHandler> handlers;

	public OutboxDispatchService(OutboxEventRepository repository, List<OutboxEventHandler> handlers) {
		this.repository = repository;
		this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(OutboxEventHandler::supports, Function.identity()));
	}

	@Transactional
	public List<String> claimBatch(int size) {
		List<OutboxEvent> events = repository.lockDispatchable(
				EnumSet.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
				Instant.now(),
				PageRequest.of(0, Math.max(1, Math.min(size, 100))));
		events.forEach(OutboxEvent::claim);
		return repository.saveAll(events).stream().map(OutboxEvent::getId).toList();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(String eventId) {
		OutboxEvent event = repository.findById(eventId).orElse(null);
		if (event == null || event.getStatus() != OutboxStatus.PROCESSING) {
			return;
		}
		try {
			OutboxEventHandler handler = handlers.get(event.getEventType());
			if (handler == null) {
				throw new IllegalStateException("No outbox handler registered for " + event.getEventType());
			}
			handler.handle(event);
			event.processed();
		} catch (Exception exception) {
			event.retry(exception);
		}
		repository.save(event);
	}

	@Transactional
	public void releaseStaleLocks() {
		repository.findTop100ByStatusAndLockedAtBeforeOrderByLockedAtAsc(
				OutboxStatus.PROCESSING,
				Instant.now().minusSeconds(900L))
				.forEach(event -> {
					event.releaseStaleLock();
					repository.save(event);
				});
	}
}
