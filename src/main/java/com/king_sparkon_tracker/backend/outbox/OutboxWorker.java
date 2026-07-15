package com.king_sparkon_tracker.backend.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {

	private final OutboxDispatchService dispatchService;

	public OutboxWorker(OutboxDispatchService dispatchService) {
		this.dispatchService = dispatchService;
	}

	@Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
	public void dispatch() {
		dispatchService.claimBatch(50).forEach(dispatchService::process);
	}

	@Scheduled(fixedDelayString = "${app.outbox.stale-lock-delay-ms:300000}")
	public void releaseStaleLocks() {
		dispatchService.releaseStaleLocks();
	}
}
