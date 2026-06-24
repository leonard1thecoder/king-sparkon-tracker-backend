package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.dto.SubscribeRequest;
import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.SubscriberType;
import com.king_sparkon_tracker.backend.repository.SubscriberRepository;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

	@Mock
	private SubscriberRepository subscriberRepository;

	private SubscriberService subscriberService;

	@BeforeEach
	void setUp() {
		subscriberService = new SubscriberService(subscriberRepository);
	}

	@Test
	void directEmailSignupDefaultsToKingSparkonSubscriber() {
		when(subscriberRepository.findByContactValue("client@example.com")).thenReturn(Optional.empty());
		when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Subscriber result = subscriberService.subscribe(new SubscribeRequest(" CLIENT@EXAMPLE.COM ", null, null));

		assertThat(result.getContactValue()).isEqualTo("client@example.com");
		assertThat(result.getContactType()).isEqualTo(SubscriberContactType.EMAIL);
		assertThat(result.getSubscriberType()).isEqualTo(SubscriberType.KINGSPARKON_SUBSCRIBER);
		assertThat(result.getPreferredChannel()).isEqualTo(PromotionChannel.ANY);
		assertThat(result.getSource()).isEqualTo("DIRECT");
	}

	@Test
	void tipPaymentClientSignupUsesClientType() {
		when(subscriberRepository.findByContactValue("+27821234567")).thenReturn(Optional.empty());
		when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Subscriber result = subscriberService.subscribeTipPaymentClient(" +27 82 123 4567 ");

		assertThat(result.getContactValue()).isEqualTo("+27821234567");
		assertThat(result.getContactType()).isEqualTo(SubscriberContactType.CELLPHONE);
		assertThat(result.getSubscriberType()).isEqualTo(SubscriberType.CLIENT);
		assertThat(result.getSource()).isEqualTo("TIP_PAYMENT_LINK");
	}

	@Test
	void rejectsCellphoneWithoutInternationalFormat() {
		assertThatThrownBy(() -> subscriberService.subscribe(new SubscribeRequest("0821234567", null, PromotionChannel.WHATSAPP)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cellphone number must be in international format, for example +27821234567");
	}
}
