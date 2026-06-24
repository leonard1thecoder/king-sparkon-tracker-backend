package com.king_sparkon_tracker.backend.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.CreatePromotionRequest;
import com.king_sparkon_tracker.backend.dto.PromotionPriceQuoteResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Promotion;
import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.PromotionNotification;
import com.king_sparkon_tracker.backend.model.PromotionNotificationStatus;
import com.king_sparkon_tracker.backend.model.PromotionOrigin;
import com.king_sparkon_tracker.backend.model.PromotionStatus;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.PromotionNotificationRepository;
import com.king_sparkon_tracker.backend.repository.PromotionRepository;
import com.king_sparkon_tracker.backend.repository.SubscriberRepository;

@Service
@Transactional
public class PromotionService {

	private static final Logger log = LoggerFactory.getLogger(PromotionService.class);
	private static final int BATCH_SIZE = 200;

	private final PromotionRepository promotionRepository;
	private final PromotionNotificationRepository notificationRepository;
	private final SubscriberRepository subscriberRepository;
	private final TrackerUserService trackerUserService;
	private final PromotionPricingService pricingService;
	private final AppEmailService appEmailService;
	private final TwilioWhatsAppService whatsAppService;

	public PromotionService(
			PromotionRepository promotionRepository,
			PromotionNotificationRepository notificationRepository,
			SubscriberRepository subscriberRepository,
			TrackerUserService trackerUserService,
			PromotionPricingService pricingService,
			AppEmailService appEmailService,
			TwilioWhatsAppService whatsAppService) {
		this.promotionRepository = promotionRepository;
		this.notificationRepository = notificationRepository;
		this.subscriberRepository = subscriberRepository;
		this.trackerUserService = trackerUserService;
		this.pricingService = pricingService;
		this.appEmailService = appEmailService;
		this.whatsAppService = whatsAppService;
	}

	public Promotion createOwnerPromotion(CreatePromotionRequest request, String actorUsername) {
		TrackerUser owner = trackerUserService.getUserByUsername(actorUsername);
		if (owner.getPrivilege() == null || owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can create promotions");
		}

		Business business = trackerUserService.businessForActor(actorUsername);
		int targetCount = Math.toIntExact(Math.min(subscriberRepository.countByActiveTrue(), Integer.MAX_VALUE));
		PromotionPriceQuoteResponse quote = pricingService.quote(targetCount);
		Promotion promotion = new Promotion(
				business,
				normalizeRequired(request.title(), "Promotion title is required"),
				normalizeRequired(request.message(), "Promotion message is required"),
				normalizeOptional(request.landingUrl()),
				PromotionOrigin.OWNER,
				request.channel() == null ? PromotionChannel.ANY : request.channel(),
				targetCount,
				quote.totalPrice(),
				actorUsername,
				request.scheduledFor() == null ? OffsetDateTime.now() : request.scheduledFor(),
				OffsetDateTime.now().plusDays(7));
		Promotion savedPromotion = promotionRepository.save(promotion);
		log.info("owner_promotion_created promotionId={} businessId={} targetCount={} price={} actor={}",
				savedPromotion.getId(), business.getId(), targetCount, quote.totalPrice(), actorUsername);
		return savedPromotion;
	}

	public Promotion createAutomatedKingSparkonPromotion() {
		int targetCount = Math.toIntExact(Math.min(subscriberRepository.countByActiveTrue(), Integer.MAX_VALUE));
		PromotionPriceQuoteResponse quote = pricingService.quote(targetCount);
		Promotion promotion = new Promotion(
				null,
				"Run your stock, barcodes, tips and payments from one dashboard",
				"King Sparkon Tracker helps owners manage products, workers, barcode sales, tip payment links, reports and payouts without messy spreadsheets.",
				"https://kingsparkon.com",
				PromotionOrigin.KING_SPARKON_AUTOMATED,
				PromotionChannel.ANY,
				targetCount,
				quote.totalPrice(),
				"system",
				OffsetDateTime.now(),
				OffsetDateTime.now().plusDays(7));
		Promotion savedPromotion = promotionRepository.save(promotion);
		log.info("automated_kingsparkon_promotion_created promotionId={} targetCount={}", savedPromotion.getId(), targetCount);
		return savedPromotion;
	}

	@Transactional(readOnly = true)
	public PromotionPriceQuoteResponse quoteCurrentAudience() {
		return pricingService.quote(Math.toIntExact(Math.min(subscriberRepository.countByActiveTrue(), Integer.MAX_VALUE)));
	}

	@Transactional(readOnly = true)
	public Page<Promotion> listOwnerPromotions(Pageable pageable, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return promotionRepository.findByBusiness_IdOrderByCreatedDateDesc(business.getId(), pageable);
	}

	public void processDuePromotions() {
		OffsetDateTime now = OffsetDateTime.now();
		List<Promotion> promotions = promotionRepository.findTop20ByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(PromotionStatus.ACTIVE, now);
		for (Promotion promotion : promotions) {
			processPromotion(promotion, now);
		}
	}

	public void createAutomatedPromotionIfDue() {
		OffsetDateTime cutoff = OffsetDateTime.now().minusHours(1);
		if (promotionRepository.existsByOriginAndCreatedDateAfter(PromotionOrigin.KING_SPARKON_AUTOMATED, cutoff)) {
			return;
		}
		createAutomatedKingSparkonPromotion();
	}

	private void processPromotion(Promotion promotion, OffsetDateTime now) {
		if (promotion.getExpiresAt() != null && promotion.getExpiresAt().isBefore(now)) {
			promotion.expire();
			promotionRepository.save(promotion);
			return;
		}

		OffsetDateTime cutoff = now.minusDays(2);
		List<Subscriber> subscribers = dueSubscribers(promotion, cutoff);
		int sentCount = 0;
		for (Subscriber subscriber : subscribers) {
			if (notificationRepository.existsByPromotion_IdAndSubscriber_Id(promotion.getId(), subscriber.getId())) {
				continue;
			}
			PromotionChannel channel = resolveChannel(promotion, subscriber);
			boolean sent = sendPromotion(channel, subscriber, promotion);
			notificationRepository.save(new PromotionNotification(
					promotion,
					subscriber,
					channel,
					sent ? PromotionNotificationStatus.SENT : PromotionNotificationStatus.FAILED,
					sent ? null : "Provider disabled or failed"));
			if (sent) {
				subscriber.markNotified(now);
				subscriberRepository.save(subscriber);
				sentCount++;
			}
		}
		promotion.markProcessed(now);
		promotionRepository.save(promotion);
		log.info("promotion_processed promotionId={} selected={} sent={}", promotion.getId(), subscribers.size(), sentCount);
	}

	private List<Subscriber> dueSubscribers(Promotion promotion, OffsetDateTime cutoff) {
		PromotionChannel channel = promotion.getChannel();
		if (channel == PromotionChannel.EMAIL) {
			return subscriberRepository.findTop200ByActiveTrueAndContactTypeAndLastNotifiedAtIsNullOrActiveTrueAndContactTypeAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
					SubscriberContactType.EMAIL, cutoff, SubscriberContactType.EMAIL, cutoff);
		}
		if (channel == PromotionChannel.WHATSAPP) {
			return subscriberRepository.findTop200ByActiveTrueAndContactTypeAndLastNotifiedAtIsNullOrActiveTrueAndContactTypeAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
					SubscriberContactType.CELLPHONE, cutoff, SubscriberContactType.CELLPHONE, cutoff);
		}
		return subscriberRepository.findTop200ByActiveTrueAndLastNotifiedAtIsNullOrActiveTrueAndLastNotifiedAtBeforeOrderByCreatedDateAsc(cutoff);
	}

	private PromotionChannel resolveChannel(Promotion promotion, Subscriber subscriber) {
		if (promotion.getChannel() != PromotionChannel.ANY) {
			return promotion.getChannel();
		}
		if (subscriber.getPreferredChannel() != PromotionChannel.ANY) {
			return subscriber.getPreferredChannel();
		}
		return subscriber.getContactType() == SubscriberContactType.EMAIL ? PromotionChannel.EMAIL : PromotionChannel.WHATSAPP;
	}

	private boolean sendPromotion(PromotionChannel channel, Subscriber subscriber, Promotion promotion) {
		if (channel == PromotionChannel.EMAIL) {
			return appEmailService.sendPromotionEmail(subscriber.getContactValue(), promotion);
		}
		if (channel == PromotionChannel.WHATSAPP) {
			return whatsAppService.sendPromotion(subscriber.getContactValue(), whatsAppMessage(promotion));
		}
		return false;
	}

	private String whatsAppMessage(Promotion promotion) {
		StringBuilder builder = new StringBuilder();
		builder.append(promotion.getTitle()).append("\n\n").append(promotion.getMessage());
		if (StringUtils.hasText(promotion.getLandingUrl())) {
			builder.append("\n\n").append(promotion.getLandingUrl());
		}
		builder.append("\n\nYou will not receive more than one King Sparkon promotion every 2 days.");
		return builder.toString();
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
