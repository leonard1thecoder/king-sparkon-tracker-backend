package com.king_sparkon_tracker.backend.dto;

import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.SubscriberType;

public record AffiliateLeadResponse(
		Long id,
		String contactValue,
		SubscriberContactType contactType,
		SubscriberType subscriberType,
		PromotionChannel preferredChannel,
		String source,
		String niche,
		String opportunity,
		boolean affiliateRegistered,
		OffsetDateTime createdDate
) {
	public static AffiliateLeadResponse from(Subscriber subscriber) {
		return new AffiliateLeadResponse(
				subscriber.getId(),
				subscriber.getContactValue(),
				subscriber.getContactType(),
				subscriber.getSubscriberType(),
				subscriber.getPreferredChannel(),
				subscriber.getSource(),
				niche(subscriber),
				opportunity(subscriber),
				subscriber.isAffiliateRegistered(),
				subscriber.getCreatedDate());
	}

	private static String niche(Subscriber subscriber) {
		String source = normalizedSource(subscriber);
		if (source.contains("TIP")) return "Hospitality, service teams and digital tips";
		if (source.contains("WEBSITE") || source.contains("PAYMENT")) return "Retail, ecommerce and product checkout";
		if (source.contains("TICKET")) return "Events, venues and ticket operations";
		if (source.contains("JOB")) return "Recruitment and job opportunities";
		if (subscriber.getSubscriberType() == SubscriberType.AFFILIATE) return "Affiliate marketing and business referrals";
		if (subscriber.getSubscriberType() == SubscriberType.CLIENT) return "Small-business customer operations";
		return "General SME growth and operations";
	}

	private static String opportunity(Subscriber subscriber) {
		String source = normalizedSource(subscriber);
		if (subscriber.getSubscriberType() == SubscriberType.AFFILIATE && !subscriber.isAffiliateRegistered()) {
			return "Invite this lead to create an affiliate account, then help them share Plus and Pro business subscriptions.";
		}
		if (source.contains("TIP")) {
			return "Lead with worker QR tips, then introduce the business dashboard and earn commission when the owner subscribes to Plus or Pro.";
		}
		if (source.contains("WEBSITE") || source.contains("PAYMENT")) {
			return "Lead with barcode stock, product checkout and payment tracking. Use your referral link for the owner subscription.";
		}
		if (source.contains("TICKET")) {
			return "Show QR ticket sales and entry verification, then refer the event business to a paid plan through your tracked link.";
		}
		if (source.contains("JOB")) {
			return "Show job posting and applicant workflows, then refer the hiring business through your affiliate pricing link.";
		}
		return "Contact the lead through their preferred channel, identify the operational pain point, and send your tracked pricing link before signup.";
	}

	private static String normalizedSource(Subscriber subscriber) {
		return subscriber.getSource() == null ? "" : subscriber.getSource().trim().toUpperCase();
	}
}
