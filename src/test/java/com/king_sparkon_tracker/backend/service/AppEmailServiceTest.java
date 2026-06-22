package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.king_sparkon_tracker.backend.model.AdminBusinessOverrideAction;
import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionType;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class AppEmailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private TemplateEngine templateEngine;

	private AppEmailService appEmailService;
	private Business business;
	private TrackerUser owner;
	private TrackerUser worker;

	@BeforeEach
	void setUp() {
		appEmailService = new AppEmailService(
				mailSender,
				templateEngine,
				true,
				"support@example.com",
				"http://localhost:3000/login");
		when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");
		when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));

		owner = new TrackerUser("owner", "owner@example.com", "encoded", new Privilege(PrivilegeRole.Owner));
		worker = new TrackerUser("worker", "worker@example.com", "encoded", new Privilege(PrivilegeRole.Worker));
		business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		worker.setBusiness(business);
	}

	@Test
	void notificationMethodsRenderExpectedTemplatesAndSendMail() {
		Product product = product();
		ProductBarcode barcode = barcode(product);
		InventoryTransaction transaction = transaction(product);
		BusinessSubscription subscription = subscription();

		assertThat(appEmailService.sendWorkerCreatedEmail(worker, business)).isTrue();
		assertThat(appEmailService.sendProductApprovalRequestEmail(business, product, "worker", 2L)).isTrue();
		assertThat(appEmailService.sendTransactionCreatedOwnerEmail(transaction)).isTrue();
		assertThat(appEmailService.sendTransactionCreatedWorkerEmail(transaction)).isTrue();
		assertThat(appEmailService.sendBarcodeClaimedEmail(barcode, "worker")).isTrue();
		assertThat(appEmailService.sendReturnableBarcodesExpiredEmail(business, 3L, "system")).isTrue();
		assertThat(appEmailService.sendBillingActivatedEmail(business, subscription, "Activated")).isTrue();
		assertThat(appEmailService.sendBillingPaymentFailedEmail(business, subscription, "Payment failed")).isTrue();
		assertThat(appEmailService.sendBillingCancelledEmail(business, subscription, "Cancelled")).isTrue();
		assertThat(appEmailService.sendBillingSuspendedEmail(business, subscription, "Suspended")).isTrue();
		assertThat(appEmailService.sendBillingExpiredEmail(business, subscription, "Expired")).isTrue();
		assertThat(appEmailService.sendBusinessDeactivatedEmail(business, "Deactivated")).isTrue();
		assertThat(appEmailService.sendAdminBusinessStatusChangedEmail(
				business,
				AdminBusinessOverrideAction.DEACTIVATE,
				"trial abuse",
				"admin")).isTrue();

		verify(templateEngine).process(eq("email/worker-created"), any(Context.class));
		verify(templateEngine).process(eq("email/product-approval-request"), any(Context.class));
		verify(templateEngine).process(eq("email/transaction-created-owner"), any(Context.class));
		verify(templateEngine).process(eq("email/transaction-created-worker"), any(Context.class));
		verify(templateEngine).process(eq("email/barcode-claimed"), any(Context.class));
		verify(templateEngine).process(eq("email/returnable-barcodes-expired"), any(Context.class));
		verify(templateEngine).process(eq("email/billing-activated"), any(Context.class));
		verify(templateEngine).process(eq("email/billing-payment-failed"), any(Context.class));
		verify(templateEngine).process(eq("email/billing-cancelled"), any(Context.class));
		verify(templateEngine).process(eq("email/billing-suspended"), any(Context.class));
		verify(templateEngine).process(eq("email/billing-expired"), any(Context.class));
		verify(templateEngine).process(eq("email/business-deactivated"), any(Context.class));
		verify(templateEngine).process(eq("email/admin-business-status-changed"), any(Context.class));
		verify(mailSender, times(13)).send(any(MimeMessage.class));
	}

	private Product product() {
		Product product = new Product(
				"Beer",
				ProductCategory.Alcohol,
				new BigDecimal("20.00"),
				2,
				true,
				new BigDecimal("3.00"),
				false,
				BigDecimal.ZERO,
				null,
				null,
				business);
		ReflectionTestUtils.setField(product, "id", 9L);
		return product;
	}

	private ProductBarcode barcode(Product product) {
		ProductBarcode barcode = new ProductBarcode("6001");
		barcode.setReferencee("0821234567");
		product.addBarcode(barcode);
		ReflectionTestUtils.setField(barcode, "id", 14L);
		return barcode;
	}

	private InventoryTransaction transaction(Product product) {
		InventoryTransaction transaction = new InventoryTransaction(TransactionType.SELL, worker, owner, business);
		transaction.setDate(LocalDateTime.of(2026, 6, 1, 12, 0));
		transaction.addItem(new TransactionItem(product, 1, new BigDecimal("20.00"), List.of("6001")));
		return transaction;
	}

	private BusinessSubscription subscription() {
		BusinessSubscription subscription = new BusinessSubscription(
				business,
				BusinessPlan.PLUS,
				BillingInterval.MONTHLY,
				null,
				new BigDecimal("2300.00"),
				"ZAR");
		subscription.activate(LocalDateTime.of(2026, 6, 1, 12, 0), LocalDateTime.of(2026, 7, 1, 12, 0));
		return subscription;
	}
}
