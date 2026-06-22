package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.config.StripeProperties;
import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeBillingClient {

	private final StripeProperties properties;

	public StripeBillingClient(StripeProperties properties) {
		this.properties = properties;
	}

	public CreatedStripeCheckoutSession createCheckoutSession(
			Business business,
			BusinessSubscription subscription) {
		try {
			Session session = Session.create(
					checkoutSessionParams(business, subscription),
					RequestOptions.builder()
							.setApiKey(properties.requireSecretKey())
							.build()
			);

			if (session.getId() == null || session.getUrl() == null) {
				throw new IllegalStateException("Stripe checkout session creation failed");
			}

			return new CreatedStripeCheckoutSession(
					session.getId(),
					session.getUrl(),
					inlinePriceReference(subscription)
			);
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe checkout session creation failed", exception);
		}
	}

	public Event constructEvent(String rawPayload, String stripeSignature) throws SignatureVerificationException {
		return Webhook.constructEvent(rawPayload, stripeSignature, properties.requireWebhookSecret());
	}

	private SessionCreateParams checkoutSessionParams(Business business, BusinessSubscription subscription) {
		Map<String, String> metadata = Map.of(
				"businessId", String.valueOf(business.getId()),
				"subscriptionId", String.valueOf(subscription.getId()),
				"businessPlan", subscription.getBusinessPlan().name(),
				"billingInterval", subscription.getBillingInterval().name(),
				"termYears", String.valueOf(subscription.getTermYears() == null ? 1 : subscription.getTermYears())
		);

		return SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setSuccessUrl(properties.getSuccessUrl())
				.setCancelUrl(properties.getCancelUrl())
				.setClientReferenceId("business:%s:subscription:%s".formatted(business.getId(), subscription.getId()))
				.setCustomerEmail(business.getOwner().getEmailAddress())
				.putAllMetadata(metadata)
				.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
						.putAllMetadata(metadata)
						.build())
				.addLineItem(SessionCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(priceData(subscription))
						.build())
				.build();
	}

	private SessionCreateParams.LineItem.PriceData priceData(BusinessSubscription subscription) {
		SessionCreateParams.LineItem.PriceData.Recurring.Interval interval =
				subscription.getBillingInterval() == BillingInterval.MONTHLY
						? SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH
						: SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR;
		long intervalCount = subscription.getBillingInterval() == BillingInterval.MONTHLY
				? 1L
				: subscription.getTermYears() == null ? 1L : subscription.getTermYears().longValue();

		return SessionCreateParams.LineItem.PriceData.builder()
				.setCurrency(subscription.getCurrency().toLowerCase(Locale.ROOT))
				.setUnitAmount(unitAmountInCents(subscription.getAmount()))
				.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
						.setName("King Sparkon Tracker " + subscription.getBusinessPlan() + " subscription")
						.build())
				.setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
						.setInterval(interval)
						.setIntervalCount(intervalCount)
						.build())
				.build();
	}

	private long unitAmountInCents(BigDecimal amount) {
		return amount.movePointRight(2)
				.setScale(0, RoundingMode.HALF_UP)
				.longValueExact();
	}

	private String inlinePriceReference(BusinessSubscription subscription) {
		return "%s-%s-%s".formatted(
				subscription.getBusinessPlan(),
				subscription.getBillingInterval(),
				subscription.getTermYears() == null ? 1 : subscription.getTermYears()
		);
	}

	public record CreatedStripeCheckoutSession(
			String checkoutSessionId,
			String checkoutUrl,
			String priceReference
	) {
	}
}
