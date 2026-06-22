package com.king_sparkon_tracker.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

	private String secretKey;

	private String webhookSecret;

	private String successUrl = "http://localhost:3000/dashboard/owner?billing=stripe-success";

	private String cancelUrl = "http://localhost:3000/dashboard/owner?billing=stripe-cancelled";

	public String requireSecretKey() {
		if (secretKey == null || secretKey.isBlank()) {
			throw new IllegalStateException("Stripe secret key is not configured");
		}

		return secretKey;
	}

	public String requireWebhookSecret() {
		if (webhookSecret == null || webhookSecret.isBlank()) {
			throw new IllegalStateException("Stripe webhook secret is not configured");
		}

		return webhookSecret;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public void setWebhookSecret(String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public String getSuccessUrl() {
		return successUrl;
	}

	public void setSuccessUrl(String successUrl) {
		this.successUrl = successUrl;
	}

	public String getCancelUrl() {
		return cancelUrl;
	}

	public void setCancelUrl(String cancelUrl) {
		this.cancelUrl = cancelUrl;
	}
}
