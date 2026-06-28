package com.king_sparkon_tracker.backend.tickets.service;

import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TicketStripeCheckoutService {

    private final String stripeSecretKey;
    private final String stripePublishableKey;
    private final String successUrl;
    private final String cancelUrl;

    public TicketStripeCheckoutService(
            @Value("${stripe.api-key:}") String stripeSecretKey,
            @Value("${stripe.publishable-key:}") String stripePublishableKey,
            @Value("${app.tickets.checkout-success-url:http://localhost:3000/tickets/my-tickets?stripe=success&session_id={CHECKOUT_SESSION_ID}}") String successUrl,
            @Value("${app.tickets.checkout-cancel-url:http://localhost:3000/tickets?stripe=cancelled}") String cancelUrl
    ) {
        this.stripeSecretKey = stripeSecretKey;
        this.stripePublishableKey = stripePublishableKey;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    public Session createTicketCheckoutSession(String paymentId, String eventName, TicketType ticketType, int quantity, BigDecimal totalAmount) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key is not configured.");
        }
        Stripe.apiKey = stripeSecretKey;
        long amountInCents = totalAmount.movePointRight(2).longValueExact();
        SessionCreateParams.LineItem.PriceData.ProductData productData = SessionCreateParams.LineItem.PriceData.ProductData.builder()
                .setName(eventName + " - " + ticketType.name() + " ticket")
                .build();
        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("zar")
                .setUnitAmount(amountInCents)
                .setProductData(productData)
                .build();
        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(priceData)
                .build();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(lineItem)
                .putAllMetadata(Map.of("paymentId", paymentId, "quantity", String.valueOf(quantity), "ticketType", ticketType.name()))
                .build();
        try {
            return Session.create(params);
        } catch (StripeException exception) {
            throw new IllegalStateException("Unable to create Stripe ticket checkout session.", exception);
        }
    }

    public Session retrieveSession(String sessionId) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key is not configured.");
        }
        Stripe.apiKey = stripeSecretKey;
        try {
            return Session.retrieve(sessionId);
        } catch (StripeException exception) {
            throw new IllegalStateException("Unable to retrieve Stripe checkout session.", exception);
        }
    }

    public String publishableKey() {
        return stripePublishableKey;
    }
}
