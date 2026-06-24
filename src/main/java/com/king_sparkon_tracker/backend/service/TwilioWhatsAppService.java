package com.king_sparkon_tracker.backend.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TwilioWhatsAppService {

	private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppService.class);

	private final HttpClient httpClient;
	private final boolean enabled;
	private final String accountSid;
	private final String authToken;
	private final String whatsappFrom;

	public TwilioWhatsAppService(
			@Value("${twilio.whatsapp.enabled:false}") boolean enabled,
			@Value("${twilio.account-sid:}") String accountSid,
			@Value("${twilio.auth-token:}") String authToken,
			@Value("${twilio.whatsapp.from:}") String whatsappFrom) {
		this.enabled = enabled;
		this.accountSid = accountSid;
		this.authToken = authToken;
		this.whatsappFrom = whatsappFrom;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
	}

	public boolean sendPromotion(String cellphoneNumber, String message) {
		String normalizedTo = normalizeWhatsAppNumber(cellphoneNumber);
		if (!enabled) {
			log.warn("twilio_whatsapp_preview enabled=false to={} message={}", maskPhone(normalizedTo), message);
			return false;
		}
		if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken) || !StringUtils.hasText(whatsappFrom)) {
			throw new IllegalStateException("Twilio WhatsApp is enabled but account SID, auth token, or from number is missing");
		}

		try {
			String body = "From=" + encode(normalizeWhatsAppNumber(whatsappFrom))
					+ "&To=" + encode(normalizedTo)
					+ "&Body=" + encode(message);
			String credentials = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Basic " + credentials)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				log.info("twilio_whatsapp_sent to={}", maskPhone(normalizedTo));
				return true;
			}
			log.warn("twilio_whatsapp_failed status={} to={} body={}", response.statusCode(), maskPhone(normalizedTo), response.body());
			return false;
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Twilio WhatsApp send interrupted", interruptedException);
		} catch (Exception exception) {
			log.error("twilio_whatsapp_failed to={}", maskPhone(normalizedTo), exception);
			return false;
		}
	}

	private String normalizeWhatsAppNumber(String number) {
		String value = number == null ? "" : number.trim();
		if (value.startsWith("whatsapp:")) {
			return value;
		}
		return "whatsapp:" + value;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String maskPhone(String value) {
		if (value == null || value.length() < 8) {
			return "***";
		}
		return value.substring(0, Math.min(12, value.length())) + "***" + value.substring(value.length() - 3);
	}
}
