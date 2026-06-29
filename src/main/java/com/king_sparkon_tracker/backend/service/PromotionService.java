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
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Promotion;
import com.king_sparkon_tracker.backend.model.PromotionAudience;
import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.PromotionNotification;
import com.king_sparkon_tracker.backend.model.PromotionNotificationStatus;
import com.king_sparkon_tracker.backend.model.PromotionOrigin;
import com.king_sparkon_tracker.backend.model.PromotionStatus;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.SubscriberType;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.PromotionNotificationRepository;
import com.king_sparkon_tracker.backend.repository.PromotionRepository;
import com.king_sparkon_tracker.backend.repository.SubscriberRepository;

@Service
@Transactional
public class PromotionService {

	private static final Logger log = LoggerFactory.getLogger(PromotionService.class);

	private final PromotionRepository promotionRepository;
	private final PromotionNotificationRepository notificationRepository;
	private final SubscriberRepository subscriberRepository;
	private final TrackerUserService trackerUserService;
	private final PromotionPricingService pricingService;
	private final BusinessAccountService businessAccountService;
	private final AppEmailService appEmailService;
	private final TwilioWhatsAppService whatsAppService;

	public PromotionService(
			PromotionRepository promotionRepository,
			PromotionNotificationRepository notificationRepository,
			SubscriberRepository subscriberRepository,
			TrackerUserService trackerUserService,
			PromotionPricingService pricingService,
			BusinessAccountService businessAccountService,
			AppEmailService appEmailService,
			TwilioWhatsAppService whatsAppService) {
		this.promotionRepository = promotionRepository;
		this.notificationRepository = notificationRepository;
		this.subscriberRepository = subscriberRepository;
		this.trackerUserService = trackerUserService;
		this.pricingService = pricingService;
		this.businessAccountService = businessAccountService;
		this.appEmailService = appEmailService;
		this.whatsAppService = whatsAppService;
	}

	public Promotion createOwnerPromotion(CreatePromotionRequest request, String actorUsername) {
		TrackerUser owner = trackerUserService.getUserByUsername(actorUsername);
		if (owner.getPrivilege() == null || owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can create promotions");
		}

		Business business = trackerUserService.businessForActor(actorUsername);
		return createPromotion(
				business,
				request,
				PromotionOrigin.OWNER,
				PromotionAudience.ALL_SUBSCRIBERS,
				actorUsername);
	}

	public Promotion createAdminRegisteredSubscriberPromotion(CreatePromotionRequest request, String actorUsername) {
		TrackerUser admin = trackerUserService.getUserByUsername(actorUsername);
		if (admin.getPrivilege() == null || admin.getPrivilege().getName() != PrivilegeRole.Admin) {
			throw new IllegalArgumentException("Only administrators can create registered-subscriber promotions");
		}
		return createPromotion(
				null,
				request,
				PromotionOrigin.KING_SPARKON_AUTOMATED,
				PromotionAudience.REGISTERED_SUBSCRIBERS,
				actorUsername);
	}

	private Promotion createPromotion(
			Business business,
			CreatePromotionRequest request,
			PromotionOrigin origin,
			PromotionAudience audience,
			String createdBy) {
		String title = normalizeRequired(request.title(), "Promotion title is required");
		String message = normalizeRequired(request.message(), "Promotion message is required");
		int targetCount = targetCountFor(audience);
		PromotionPriceQuoteResponse quote = pricingService.quote(targetCount);

		if (business != null) {
			businessAccountService.debitPromotion(
					business,
					quote.totalPrice(),
					BusinessAccountEntryType.PROMOTION_DEBIT,
					"Promotion campaign: " + title,
					createdBy);
		}

		Promotion promotion = new Promotion(
				business,
				title,
				message,
				normalizeOptional(request.landingUrl()),
				origin,
				request.channel() == null ? PromotionChannel.ANY : request.channel(),
				audience,
				targetCount,
				quote.totalPrice(),
				createdBy,
				request.scheduledFor() == null ? OffsetDateTime.now() : request.scheduledFor(),
				OffsetDateTime.now().plusDays(7));
		Promotion savedPromotion = promotionRepository.save(promotion);
		log.info("promotion_created promotionId={} audience={} targetCount={} price={} actor={}",
				savedPromotion.getId(), audience, targetCount, quote.totalPrice(), createdBy);
		return savedPromotion;
	}

	public Promotion createAutomatedKingSparkonPromotion() {
		int targetCount = targetCountFor(PromotionAudience.ALL_SUBSCRIBERS);
		PromotionPriceQuoteResponse quote = pricingService.quote(targetCount);
		Promotion promotion = new Promotion(
				null,
				"Run your stock, barcodes, tips and payments from one dashboard",
				"King Sparkon Tracker helps owners manage products, workers, barcode sales, tip payment links, reports and payouts without messy spreadsheets.",
				"https://kingsparkon.com",
				PromotionOrigin.KING_SPARKON_AUTOMATED,
				PromotionChannel.ANY,
				PromotionAudience.ALL_SUBSCRIBERS,
				targetCount,
				quote.totalPrice(),
				"system",
				OffsetDateTime.now(),
				OffsetDateTime.now().plusDays(7));
		Promotion savedPromotion = promotionRepository.save(promotion);
		log.info("automated_kingsparkon_promotion_created promotionId={} targetCount={}", savedPromotion.getId(), targetCount);
		return savedPromotion;
	}

	public Promotion createAutomatedAffiliateProgramPromotion() {
		int targetCount = targetCountFor(PromotionAudience.UNREGISTERED_AFFILIATES);
		PromotionPriceQuoteResponse quote = pricingService.quote(targetCount);
		Promotion promotion = new Promotion(
				null,
				"Earn from King Sparkon Tracker as an affiliate",
				"Register as a King Sparkon affiliate, promote worker tips and the affiliate program, and earn from referred businesses.",
				"https://kingsparkon.com/affiliates",
				PromotionOrigin.KING_SPARKON_AUTOMATED,
				PromotionChannel.ANY,
				PromotionAudience.UNREGISTERED_AFFILIATES,
				targetCount,
				quote.totalPrice(),
				"system",
				OffsetDateTime.now(),
				OffsetDateTime.now().plusDays(7));
		Promotion savedPromotion = promotionRepository.save(promotion);
		log.info("automated_affiliate_program_promotion_created promotionId={} targetCount={}", savedPromotion.getId(), targetCount);
		return savedPromotion;
	}

	@Transactional(readOnly = true)
	public PromotionPriceQuoteResponse quoteCurrentAudience() {
		return pricingService.quote(targetCountFor(PromotionAudience.ALL_SUBSCRIBERS));
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
		createAutomatedAffiliateProgramPromotion();
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
		log.info("promotion_processed promotionId={} audience={} selected={} sent={}", promotion.getId(), promotion.getAudience(), subscribers.size(), sentCount);
	}

	private List<Subscriber> dueSubscribers(Promotion promotion, OffsetDateTime cutoff) {
		if (promotion.getAudience() == PromotionAudience.UNREGISTERED_AFFILIATES) {
			return subscriberRepository.findTop200ByActiveTrueAndSubscriberTypeAndAffiliateRegisteredAndLastNotifiedAtIsNullOrActiveTrueAndSubscriberTypeAndAffiliateRegisteredAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
					SubscriberType.AFFILIATE, false, cutoff, SubscriberType.AFFILIATE, false, cutoff);
		}
		if (promotion.getAudience() == PromotionAudience.REGISTERED_AFFILIATES) {
			return subscriberRepository.findTop200ByActiveTrueAndSubscriberTypeAndAffiliateRegisteredAndLastNotifiedAtIsNullOrActiveTrueAndSubscriberTypeAndAffiliateRegisteredAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
					SubscriberType.AFFILIATE, true, cutoff, SubscriberType.AFFILIATE, true, cutoff);
		}
		if (promotion.getAudience() == PromotionAudience.REGISTERED_SUBSCRIBERS) {
			return subscriberRepository.findTop200ByActiveTrueAndAffiliateRegisteredAndLastNotifiedAtIsNullOrActiveTrueAndAffiliateRegisteredAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
					true, cutoff, true, cutoff);
		}
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

	private int targetCountFor(PromotionAudience audience) {
		long count;
		if (audience == PromotionAudience.UNREGISTERED_AFFILIATES) {
			count = subscriberRepository.countByActiveTrueAndSubscriberTypeAndAffiliateRegistered(SubscriberType.AFFILIATE, false);
		} else if (audience == PromotionAudience.REGISTERED_AFFILIATES) {
			count = subscriberRepository.countByActiveTrueAndSubscriberTypeAndAffiliateRegistered(SubscriberType.AFFILIATE, true);
		} else if (audience == PromotionAudience.REGISTERED_SUBSCRIBERS) {
			count = subscriberRepository.countByActiveTrueAndAffiliateRegistered(true);
		} else {
			count = subscriberRepository.countByActiveTrue();
		}
		return Math.toIntExact(Math.min(count, Integer.MAX_VALUE));
	}

	private PromotionChannel resolveChannel(Promotion promotion, Subscriber subscriber) {
		if (promotion.getChannel() == PromotionChannel.EMAIL) {
			return PromotionChannel.EMAIL;
		}
		if (promotion.getChannel() == PromotionChannel.WHATSAPP) {
			return PromotionChannel.WHATSAPP;
		}
		return subscriber.getContactType() == SubscriberContactType.EMAIL ? PromotionChannel.EMAIL : PromotionChannel.WHATSAPP;
	}

	private boolean sendPromotion(PromotionChannel channel, Subscriber subscriber, Promotion promotion) {
		String landingUrl = promotion.getLandingUrl();
		String contactValue = subscriber.getContactValue();
		if (channel == PromotionChannel.EMAIL) {
			return appEmailService.sendPromotionEmail(contactValue, promotion.getTitle(), promotion.getMessage(), landingUrl);
		}
		return whatsAppService.sendPromotion(contactValue, promotion.getTitle(), promotion.getMessage(), landingUrl);
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
