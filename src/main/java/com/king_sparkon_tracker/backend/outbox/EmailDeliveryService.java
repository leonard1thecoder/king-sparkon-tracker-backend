package com.king_sparkon_tracker.backend.outbox;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

	private final JavaMailSender mailSender;
	private final boolean mailEnabled;
	private final String mailFrom;

	public EmailDeliveryService(
			JavaMailSender mailSender,
			@Value("${app.mail.enabled:false}") boolean mailEnabled,
			@Value("${app.mail.from:no-reply@kingsparkon-tracker.com}") String mailFrom) {
		this.mailSender = mailSender;
		this.mailEnabled = mailEnabled;
		this.mailFrom = mailFrom;
	}

	public void deliver(OutboxPayloads.Email email) {
		String to = normalize(email.to());
		if (!mailEnabled) {
			log.info("outbox_email_preview event={} recipient={} subject={}", email.eventName(), mask(to), email.subject());
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			helper.setFrom(mailFrom);
			helper.setTo(to);
			helper.setSubject(email.subject());
			helper.setText(email.html(), true);
			mailSender.send(message);
			log.info("outbox_email_sent event={} recipient={}", email.eventName(), mask(to));
		} catch (Exception exception) {
			log.error("outbox_email_failed event={} recipient={}", email.eventName(), mask(to), exception);
			throw new IllegalStateException("Could not deliver queued email", exception);
		}
	}

	private String normalize(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email recipient is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String mask(String email) {
		int at = email.indexOf('@');
		return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
	}
}
