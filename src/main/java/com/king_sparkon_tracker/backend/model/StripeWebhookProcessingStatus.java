package com.king_sparkon_tracker.backend.model;

public enum StripeWebhookProcessingStatus {
	RECEIVED,
	PROCESSED,
	DUPLICATE,
	FAILED,
	SIGNATURE_FAILED,
	IGNORED
}
