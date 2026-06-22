package com.king_sparkon_tracker.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.king_sparkon_tracker.backend.model.AdminBusinessOverrideAction;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionItem;

import jakarta.mail.internet.MimeMessage;

@Service
public class AppEmailService {

	private static final Logger log = LoggerFactory.getLogger(AppEmailService.class);
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final boolean mailEnabled;
	private final String mailFrom;
	private final String frontendLoginUrl;

	public AppEmailService(
			JavaMailSender mailSender,
			TemplateEngine templateEngine,
			@Value("${app.mail.enabled:false}") boolean mailEnabled,
			@Value("${app.mail.from:no-reply@kingsparkon-tracker.com}") String mailFrom,
			@Value("${app.frontend.login-url:http://localhost:3000/login}") String frontendLoginUrl) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.mailEnabled = mailEnabled;
		this.mailFrom = mailFrom;
		this.frontendLoginUrl = frontendLoginUrl;
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

	static String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return "***";
		}
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

	public boolean sendWorkerCreatedEmail(TrackerUser worker, Business business) {
		Context context = businessContext(business);
		context.setVariable("workerUsername", worker.getUsername());
		context.setVariable("workerEmail", worker.getEmailAddress());
		context.setVariable("loginUrl", frontendLoginUrl);

		String html = templateEngine.process("email/worker-created", context);
		return sendHtmlEmail(
				normalizeEmail(worker.getEmailAddress()),
				"You have been added to King Sparkon Tracker",
				html,
				"worker_created_email");
	}

	public boolean sendProductApprovalRequestEmail(Business business, Product product, String actorUsername, long barcodeCount) {
		Context context = businessContext(business);
		context.setVariable("productName", product.getName());
		context.setVariable("productCategory", product.getCategory());
		context.setVariable("stockQuantity", product.getStockQuantity());
		context.setVariable("barcodeCount", barcodeCount);
		context.setVariable("actorUsername", actorUsername);

		String html = templateEngine.process("email/product-approval-request", context);
		return sendHtmlEmail(
				ownerEmail(business),
				"Product approval required",
				html,
				"product_approval_request_email");
	}

	public boolean sendTransactionCreatedOwnerEmail(InventoryTransaction transaction) {
		Context context = transactionContext(transaction);

		String html = templateEngine.process("email/transaction-created-owner", context);
		return sendHtmlEmail(
				normalizeEmail(transaction.getOwner().getEmailAddress()),
				"New King Sparkon Tracker transaction",
				html,
				"transaction_created_owner_email");
	}

	public boolean sendTransactionCreatedWorkerEmail(InventoryTransaction transaction) {
		Context context = transactionContext(transaction);

		String html = templateEngine.process("email/transaction-created-worker", context);
		return sendHtmlEmail(
				normalizeEmail(transaction.getEmployee().getEmailAddress()),
				"Transaction recorded in King Sparkon Tracker",
				html,
				"transaction_created_worker_email");
	}

	public boolean sendBarcodeClaimedEmail(ProductBarcode barcode, String actorUsername) {
		Business business = barcode.getProduct().getBusiness();
		Context context = businessContext(business);
		context.setVariable("productName", barcode.getProduct().getName());
		context.setVariable("barcode", barcode.getBarcode());
		context.setVariable("reference", barcode.getReferencee());
		context.setVariable("actorUsername", actorUsername);

		String html = templateEngine.process("email/barcode-claimed", context);
		return sendHtmlEmail(
				ownerEmail(business),
				"Returnable barcode claimed",
				html,
				"barcode_claimed_email");
	}

	public boolean sendReturnableBarcodesExpiredEmail(Business business, long expiredCount, String actorUsername) {
		Context context = businessContext(business);
		context.setVariable("expiredCount", expiredCount);
		context.setVariable("actorUsername", actorUsername);

		String html = templateEngine.process("email/returnable-barcodes-expired", context);
		return sendHtmlEmail(
				ownerEmail(business),
				"Returnable barcodes expired",
				html,
				"returnable_barcodes_expired_email");
	}

	public boolean sendBillingActivatedEmail(Business business, BusinessSubscription subscription, String message) {
		Context context = billingContext(business, subscription, message);
		String html = templateEngine.process("email/billing-activated", context);
		return sendHtmlEmail(ownerEmail(business), "King Sparkon Tracker billing activated", html, "billing_activated_email");
	}

	public boolean sendBillingPaymentFailedEmail(Business business, BusinessSubscription subscription, String message) {
		Context context = billingContext(business, subscription, message);
		String html = templateEngine.process("email/billing-payment-failed", context);
		return sendHtmlEmail(ownerEmail(business), "King Sparkon Tracker payment failed", html, "billing_payment_failed_email");
	}

	public boolean sendBillingCancelledEmail(Business business, BusinessSubscription subscription, String message) {
		Context context = billingContext(business, subscription, message);
		String html = templateEngine.process("email/billing-cancelled", context);
		return sendHtmlEmail(ownerEmail(business), "King Sparkon Tracker subscription cancelled", html, "billing_cancelled_email");
	}

	public boolean sendBillingSuspendedEmail(Business business, BusinessSubscription subscription, String message) {
		Context context = billingContext(business, subscription, message);
		String html = templateEngine.process("email/billing-suspended", context);
		return sendHtmlEmail(ownerEmail(business), "King Sparkon Tracker subscription suspended", html, "billing_suspended_email");
	}

	public boolean sendBillingExpiredEmail(Business business, BusinessSubscription subscription, String message) {
		Context context = billingContext(business, subscription, message);
		String html = templateEngine.process("email/billing-expired", context);
		return sendHtmlEmail(ownerEmail(business), "King Sparkon Tracker subscription expired", html, "billing_expired_email");
	}

	public boolean sendBusinessDeactivatedEmail(Business business, String message) {
		Context context = businessContext(business);
		context.setVariable("businessStatus", business.getBusinessStatus());
		context.setVariable("message", message);

		String html = templateEngine.process("email/business-deactivated", context);
		return sendHtmlEmail(ownerEmail(business), "King Sparkon Tracker business deactivated", html, "business_deactivated_email");
	}

	public boolean sendAdminBusinessStatusChangedEmail(
			Business business,
			AdminBusinessOverrideAction action,
			String reason,
			String actorUsername) {
		Context context = businessContext(business);
		context.setVariable("action", action);
		context.setVariable("businessPlan", business.getBusinessPlan());
		context.setVariable("businessStatus", business.getBusinessStatus());
		context.setVariable("reason", reason == null || reason.isBlank() ? "Not provided" : reason);
		context.setVariable("actorUsername", actorUsername);

		String html = templateEngine.process("email/admin-business-status-changed", context);
		return sendHtmlEmail(
				ownerEmail(business),
				"King Sparkon Tracker business status changed",
				html,
				"admin_business_status_changed_email");
	}

	private Context businessContext(Business business) {
		Context context = new Context(Locale.getDefault());
		context.setVariable("businessName", business.getName());
		context.setVariable("ownerUsername", business.getOwner().getUsername());
		context.setVariable("ownerEmail", business.getOwner().getEmailAddress());
		context.setVariable("supportEmail", mailFrom);
		return context;
	}

	private Context transactionContext(InventoryTransaction transaction) {
		Context context = businessContext(transaction.getBusiness());
		context.setVariable("transactionType", transaction.getType());
		context.setVariable("transactionDate", transaction.getDate() == null ? "Pending" : DATE_TIME_FORMAT.format(transaction.getDate()));
		context.setVariable("employeeUsername", transaction.getEmployee().getUsername());
		context.setVariable("ownerUsername", transaction.getOwner().getUsername());
		context.setVariable("itemCount", transaction.getItems().size());
		context.setVariable("items", transaction.getItems().stream()
				.map(this::transactionItemSummary)
				.toList());
		return context;
	}

	private Context billingContext(Business business, BusinessSubscription subscription, String message) {
		Context context = businessContext(business);
		context.setVariable("businessPlan", business.getBusinessPlan());
		context.setVariable("businessStatus", business.getBusinessStatus());
		context.setVariable("paymentStatus", subscription == null ? null : subscription.getStatus());
		context.setVariable("billingInterval", subscription == null ? null : subscription.getBillingInterval());
		context.setVariable("periodEnd", business.getCurrentBillingPeriodEndDate() == null
				? "Not set"
				: DATE_TIME_FORMAT.format(business.getCurrentBillingPeriodEndDate()));
		context.setVariable("message", message);
		return context;
	}

	private TransactionItemSummary transactionItemSummary(TransactionItem item) {
		return new TransactionItemSummary(
				item.getProduct().getName(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getBarcodes());
	}

	private String ownerEmail(Business business) {
		return normalizeEmail(business.getOwner().getEmailAddress());
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
			throw new IllegalStateException("Could not send email. Please try again later.");
		}
	}

	public record TransactionItemSummary(
			String productName,
			int quantity,
			java.math.BigDecimal unitPrice,
			java.util.List<String> barcodes) {
	}
}
