package com.king_sparkon_tracker.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
public class AppEmailService {

	private static final Logger log = LoggerFactory.getLogger(AppEmailService.class);

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final boolean mailEnabled;
	private final String mailFrom;

	public AppEmailService(
			JavaMailSender mailSender,
			TemplateEngine templateEngine,
			@Value("${app.mail.enabled:false}") boolean mailEnabled,
			@Value("${app.mail.from:no-reply@kingsparkon-tracker.com}") String mailFrom) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.mailEnabled = mailEnabled;
		this.mailFrom = mailFrom;
	}

	public void sendPasswordResetEmail(
			String to,
			String username,
			String resetUrl,
			long expiresInMinutes) {
		String normalizedTo = normalizeEmail(to);

		Context context = new Context(Locale.getDefault());
		context.setVariable("username", username);
		context.setVariable("resetUrl", resetUrl);
		context.setVariable("expiresInMinutes", expiresInMinutes);
		context.setVariable("supportEmail", mailFrom);

		String html = templateEngine.process("email/password-reset", context);

		if (!mailEnabled) {
			log.warn(
					"password_reset_email_preview mailEnabled=false recipient={} resetUrl={}",
					maskEmail(normalizedTo),
					resetUrl);
			return;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());

			helper.setFrom(mailFrom);
			helper.setTo(normalizedTo);
			helper.setSubject("Reset your King Sparkon Tracker password");
			helper.setText(html, true);

			mailSender.send(message);
			log.info("password_reset_email_sent recipient={}", maskEmail(normalizedTo));
		} catch (Exception ex) {
			log.error("password_reset_email_failed recipient={}", maskEmail(normalizedTo), ex);
			throw new IllegalStateException("Could not send password reset email. Please try again later.");
		}
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private String maskEmail(String email) {
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}

	public void sendEmailVerificationEmail(String to, String username, String verificationUrl, long expiresInHours) {
		Context context = new Context(Locale.getDefault());
		context.setVariable("username", username);
		context.setVariable("verificationUrl", verificationUrl);
		context.setVariable("expiresInHours", expiresInHours);
		context.setVariable("supportEmail", mailFrom);
		String normalizedTo = normalizeEmail(to);
		String html = templateEngine.process("email/email-verification", context);

		if (!mailEnabled) {
			log.warn(
					"email_verification_email_preview mailEnabled=false recipient={} verificationUrl={}",
					maskEmail(normalizedTo),
					verificationUrl);
			return;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());

			helper.setFrom(mailFrom);
			helper.setTo(normalizedTo);
			helper.setSubject("Verify your King Sparkon Tracker email address");
			helper.setText(html, true);

			mailSender.send(message);
			log.info("verification_email_sent recipient={}", maskEmail(normalizedTo));
		} catch (Exception ex) {
			log.error("verification_email_failed recipient={}", maskEmail(normalizedTo), ex);
			throw new IllegalStateException("Could not send email verification. Please try again later.");
		}
	}

	public boolean sendContactInquiryConfirmationEmail(String to, String contactName, String businessName) {
		String normalizedTo = normalizeEmail(to);

		Context context = new Context(Locale.getDefault());
		context.setVariable("contactName", contactName == null || contactName.isBlank() ? businessName : contactName);
		context.setVariable("businessName", businessName);
		context.setVariable("supportEmail", mailFrom);

		String html = templateEngine.process("email/contact-confirmation", context);

		return sendHtmlEmail(
				normalizedTo,
				"We received your King Sparkon Tracker inquiry",
				html,
				"contact_confirmation_email");
	}

	public boolean sendContactInquiryNotificationEmail(
			String to,
			String businessName,
			String contactName,
			String emailAddress,
			String phoneNumber,
			String inquiryMessage) {
		String normalizedTo = normalizeEmail(to);

		Context context = new Context(Locale.getDefault());
		context.setVariable("businessName", businessName);
		context.setVariable("contactName", contactName == null || contactName.isBlank() ? "Not provided" : contactName);
		context.setVariable("emailAddress", emailAddress);
		context.setVariable("phoneNumber", phoneNumber == null || phoneNumber.isBlank() ? "Not provided" : phoneNumber);
		context.setVariable("inquiryMessage", inquiryMessage);
		context.setVariable("supportEmail", mailFrom);

		String html = templateEngine.process("email/contact-notification", context);

		return sendHtmlEmail(
				normalizedTo,
				"New King Sparkon Tracker contact inquiry",
				html,
				"contact_notification_email");
	}

	private boolean sendHtmlEmail(String to, String subject, String html, String eventName) {
		if (!mailEnabled) {
			log.warn("{}_preview mailEnabled=false recipient={} subject={}", eventName, maskEmail(to), subject);
			return false;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());

			helper.setFrom(mailFrom);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);

			mailSender.send(message);
			log.info("{}_sent recipient={}", eventName, maskEmail(to));
			return true;
		} catch (Exception ex) {
			log.error("{}_failed recipient={}", eventName, maskEmail(to), ex);
			throw new IllegalStateException("Could not send contact inquiry email. Please try again later.");
		}
	}
}
