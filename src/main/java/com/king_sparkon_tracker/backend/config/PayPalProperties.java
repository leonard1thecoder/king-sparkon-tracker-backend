package com.king_sparkon_tracker.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

@Component
@ConfigurationProperties(prefix = "paypal")
public class PayPalProperties {

	private String baseUrl = "https://api-m.sandbox.paypal.com";

	private String clientId;

	private String clientSecret;

	private String webhookId;

	private String returnUrl;

	private String cancelUrl;

	private final Billing billing = new Billing();

	public String planId(BusinessPlan businessPlan, BillingInterval billingInterval) {
		if (businessPlan == BusinessPlan.PLUS && billingInterval == BillingInterval.MONTHLY) {
			return billing.plusMonthlyPlanId;
		}

		if (businessPlan == BusinessPlan.PLUS && billingInterval == BillingInterval.YEARLY) {
			return billing.plusYearlyPlanId;
		}

		if (businessPlan == BusinessPlan.PRO && billingInterval == BillingInterval.MONTHLY) {
			return billing.proMonthlyPlanId;
		}

		if (businessPlan == BusinessPlan.PRO && billingInterval == BillingInterval.YEARLY) {
			return billing.proYearlyPlanId;
		}

		throw new IllegalArgumentException("No PayPal plan id configured for " + businessPlan + " " + billingInterval);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getWebhookId() {
		return webhookId;
	}

	public void setWebhookId(String webhookId) {
		this.webhookId = webhookId;
	}

	public String getReturnUrl() {
		return returnUrl;
	}

	public void setReturnUrl(String returnUrl) {
		this.returnUrl = returnUrl;
	}

	public String getCancelUrl() {
		return cancelUrl;
	}

	public void setCancelUrl(String cancelUrl) {
		this.cancelUrl = cancelUrl;
	}

	public Billing getBilling() {
		return billing;
	}

	public static class Billing {

		private String plusMonthlyPlanId;

		private String plusYearlyPlanId;

		private String proMonthlyPlanId;

		private String proYearlyPlanId;

		public String getPlusMonthlyPlanId() {
			return plusMonthlyPlanId;
		}

		public void setPlusMonthlyPlanId(String plusMonthlyPlanId) {
			this.plusMonthlyPlanId = plusMonthlyPlanId;
		}

		public String getPlusYearlyPlanId() {
			return plusYearlyPlanId;
		}

		public void setPlusYearlyPlanId(String plusYearlyPlanId) {
			this.plusYearlyPlanId = plusYearlyPlanId;
		}

		public String getProMonthlyPlanId() {
			return proMonthlyPlanId;
		}

		public void setProMonthlyPlanId(String proMonthlyPlanId) {
			this.proMonthlyPlanId = proMonthlyPlanId;
		}

		public String getProYearlyPlanId() {
			return proYearlyPlanId;
		}

		public void setProYearlyPlanId(String proYearlyPlanId) {
			this.proYearlyPlanId = proYearlyPlanId;
		}
	}
}
