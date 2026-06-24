package com.king_sparkon_tracker.backend.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.SubscribeRequest;
import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.SubscriberType;
import com.king_sparkon_tracker.backend.repository.SubscriberRepository;

@Service
@Transactional
public class SubscriberService {

	private static final Logger log = LoggerFactory.getLogger(SubscriberService.class);

	private final SubscriberRepository subscriberRepository;

	public SubscriberService(SubscriberRepository subscriberRepository) {
		this.subscriberRepository = subscriberRepository;
	}

	public Subscriber subscribe(SubscribeRequest request) {
		return subscribe(
				request.contact(),
				request.subscriberType() == null ? SubscriberType.KINGSPARKON_SUBSCRIBER : request.subscriberType(),
				request.preferredChannel() == null ? PromotionChannel.ANY : request.preferredChannel(),
				"DIRECT");
	}

	public Subscriber subscribeTipPaymentClient(String contact) {
		if (!StringUtils.hasText(contact)) {
			return null;
		}
		return subscribe(contact, SubscriberType.CLIENT, PromotionChannel.ANY, "TIP_PAYMENT_LINK");
	}

	public Subscriber subscribe(String rawContact, SubscriberType subscriberType, PromotionChannel preferredChannel, String source) {
		String contact = normalizeContact(rawContact);
		SubscriberContactType contactType = detectContactType(contact);
		PromotionChannel normalizedChannel = preferredChannel == null ? PromotionChannel.ANY : preferredChannel;

		if (normalizedChannel == PromotionChannel.EMAIL && contactType != SubscriberContactType.EMAIL) {
			throw new IllegalArgumentException("Email channel requires an email address subscriber contact");
		}
		if (normalizedChannel == PromotionChannel.WHATSAPP && contactType != SubscriberContactType.CELLPHONE) {
			throw new IllegalArgumentException("WhatsApp channel requires a cellphone number subscriber contact");
		}

		Subscriber subscriber = subscriberRepository.findByContactValue(contact)
				.map(existing -> {
					existing.reactivate(subscriberType, normalizedChannel, source);
					return existing;
				})
				.orElseGet(() -> new Subscriber(contact, contactType, subscriberType, normalizedChannel, source));

		Subscriber savedSubscriber = subscriberRepository.save(subscriber);
		log.info(
				"subscriber_saved subscriberId={} contactType={} subscriberType={} preferredChannel={} source={}",
				savedSubscriber.getId(),
				savedSubscriber.getContactType(),
				savedSubscriber.getSubscriberType(),
				savedSubscriber.getPreferredChannel(),
				savedSubscriber.getSource());
		return savedSubscriber;
	}

	public void unsubscribe(String rawContact) {
		String contact = normalizeContact(rawContact);
		Subscriber subscriber = subscriberRepository.findByContactValue(contact)
				.orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));
		subscriber.unsubscribe();
		subscriberRepository.save(subscriber);
	}

	private String normalizeContact(String rawContact) {
		if (!StringUtils.hasText(rawContact)) {
			throw new IllegalArgumentException("Subscriber contact is required");
		}

		String trimmed = rawContact.trim();
		if (trimmed.contains("@")) {
			return trimmed.toLowerCase(Locale.ROOT);
		}

		String compact = trimmed.replaceAll("[\\s()-]", "");
		if (!compact.startsWith("+")) {
			throw new IllegalArgumentException("Cellphone number must be in international format, for example +27821234567");
		}
		return compact;
	}

	private SubscriberContactType detectContactType(String contact) {
		if (contact.contains("@")) {
			return SubscriberContactType.EMAIL;
		}
		return SubscriberContactType.CELLPHONE;
	}
}
