package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private LocalDateTime date;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_type", length = 32)
	private TransactionPaymentType paymentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 32)
	private TransactionPaymentStatus paymentStatus = TransactionPaymentStatus.NOT_REQUIRED;

	@Column(name = "payment_email")
	private String paymentEmail;

	@Column(name = "payment_contact", length = 320)
	private String paymentContact;

	@Column(name = "payment_reference")
	private String paymentReference;

	@Column(name = "payment_url", length = 2048)
	private String paymentUrl;

	@Column(name = "payment_qr_code_url", length = 2048)
	private String paymentQrCodeUrl;

	@Column(name = "transaction_withdrawal_id")
	private Long transactionWithdrawalId;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private TrackerUser employee;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private TrackerUser owner;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "business_id")
	private Business business;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "customer_id")
	private TrackerUser customer;

	@Enumerated(EnumType.STRING)
	@Column(name = "fulfilment_status", nullable = false, length = 48)
	private TuckShopFulfilmentStatus fulfilmentStatus = TuckShopFulfilmentStatus.NOT_REQUIRED;

	@Column(name = "collection_token", unique = true, length = 96)
	private String collectionToken;

	@Column(name = "collection_ready_at")
	private LocalDateTime collectionReadyAt;

	@Column(name = "collected_at")
	private LocalDateTime collectedAt;

	@Column(name = "prepared_by_worker_id")
	private Long preparedByWorkerId;

	@OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TransactionItem> items = new ArrayList<>();

	protected InventoryTransaction() {
	}

	public InventoryTransaction(TransactionType type, TrackerUser employee, TrackerUser owner) {
		this(type, employee, owner, owner == null ? null : owner.getBusiness());
	}

	public InventoryTransaction(TransactionType type, TrackerUser employee, TrackerUser owner, Business business) {
		this.type = type;
		this.employee = employee;
		this.owner = owner;
		this.business = business;
	}

	public InventoryTransaction(
			TransactionType type,
			LocalDateTime date,
			TrackerUser employee,
			TrackerUser owner) {
		this(type, date, employee, owner, owner == null ? null : owner.getBusiness());
	}

	public InventoryTransaction(
			TransactionType type,
			LocalDateTime date,
			TrackerUser employee,
			TrackerUser owner,
			Business business) {
		this.type = type;
		this.date = date;
		this.employee = employee;
		this.owner = owner;
		this.business = business;
	}

	@PrePersist
	void beforeCreate() {
		if (date == null) {
			date = LocalDateTime.now();
		}
		if (fulfilmentStatus == null) {
			fulfilmentStatus = TuckShopFulfilmentStatus.NOT_REQUIRED;
		}
	}

	public void addItem(TransactionItem item) {
		item.setTransaction(this);
		items.add(item);
	}

	public void markOfflinePayment(TransactionPaymentType paymentType, String paymentEmail) {
		this.paymentType = paymentType;
		this.paymentStatus = TransactionPaymentStatus.NOT_REQUIRED;
		this.paymentEmail = paymentEmail;
		this.paymentContact = paymentEmail;
		this.paymentReference = null;
		this.paymentUrl = null;
		this.paymentQrCodeUrl = null;
	}

	public void prepareWebsitePayment(String paymentEmail) {
		prepareWebsitePayment(paymentEmail, paymentEmail);
	}

	public void prepareWebsitePayment(String paymentEmail, String paymentContact) {
		this.paymentType = TransactionPaymentType.WEBSITE_PAYMENT;
		this.paymentStatus = TransactionPaymentStatus.PENDING;
		this.paymentEmail = paymentEmail;
		this.paymentContact = paymentContact;
		this.paymentQrCodeUrl = null;
	}

	public void markWebsitePaymentPending(String paymentEmail, String paymentReference, String paymentUrl) {
		markWebsitePaymentPending(paymentEmail, paymentEmail, paymentReference, paymentUrl);
	}

	public void markWebsitePaymentPending(String paymentEmail, String paymentContact, String paymentReference, String paymentUrl) {
		this.paymentType = TransactionPaymentType.WEBSITE_PAYMENT;
		this.paymentStatus = TransactionPaymentStatus.PENDING;
		this.paymentEmail = paymentEmail;
		this.paymentContact = paymentContact;
		this.paymentReference = paymentReference;
		this.paymentUrl = paymentUrl;
		this.paymentQrCodeUrl = qrCodeUrl(paymentUrl);
	}

	public void markWebsitePaymentPaid(String paymentReference) {
		this.paymentType = TransactionPaymentType.WEBSITE_PAYMENT;
		this.paymentStatus = TransactionPaymentStatus.PAID;
		if ((this.paymentReference == null || this.paymentReference.isBlank()) && paymentReference != null && !paymentReference.isBlank()) {
			this.paymentReference = paymentReference;
		}
	}

	public void prepareOnlineCollection(TrackerUser customer) {
		this.customer = customer;
		this.fulfilmentStatus = TuckShopFulfilmentStatus.AWAITING_BARCODE_ASSIGNMENT;
		this.collectionToken = null;
		this.collectionReadyAt = null;
		this.collectedAt = null;
		this.preparedByWorkerId = null;
	}

	public void markReadyForCollection(Long workerId) {
		if (getOutstandingBarcodeCount() > 0) {
			throw new IllegalStateException("Every purchased unit needs a barcode before collection can be prepared");
		}
		this.fulfilmentStatus = TuckShopFulfilmentStatus.READY_FOR_COLLECTION;
		this.collectionReadyAt = LocalDateTime.now();
		this.preparedByWorkerId = workerId;
		if (collectionToken == null || collectionToken.isBlank()) {
			collectionToken = UUID.randomUUID().toString();
		}
	}

	public void markCollected() {
		if (fulfilmentStatus != TuckShopFulfilmentStatus.READY_FOR_COLLECTION) {
			throw new IllegalStateException("Only orders ready for collection can be collected");
		}
		this.fulfilmentStatus = TuckShopFulfilmentStatus.COLLECTED;
		this.collectedAt = LocalDateTime.now();
	}

	public int getOutstandingBarcodeCount() {
		return items.stream()
				.mapToInt(item -> Math.max(item.getQuantity() - item.getBarcodes().size(), 0))
				.sum();
	}

	public String getCollectionQrCodeValue() {
		if (collectionToken == null || collectionToken.isBlank()) {
			return null;
		}
		return "KST-COLLECT:" + collectionToken;
	}

	public String getCollectionQrCodeUrl() {
		return qrCodeUrl(getCollectionQrCodeValue());
	}

	public BigDecimal getTotalAmount() {
		return items.stream()
				.map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public void assignTransactionWithdrawal(Long withdrawalId) {
		this.transactionWithdrawalId = withdrawalId;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public TransactionType getType() {
		return type;
	}

	public void setType(TransactionType type) {
		this.type = type;
	}

	public TransactionPaymentType getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(TransactionPaymentType paymentType) {
		this.paymentType = paymentType;
	}

	public TransactionPaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(TransactionPaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public String getPaymentEmail() {
		return paymentEmail;
	}

	public void setPaymentEmail(String paymentEmail) {
		this.paymentEmail = paymentEmail;
	}

	public String getPaymentContact() {
		return paymentContact;
	}

	public void setPaymentContact(String paymentContact) {
		this.paymentContact = paymentContact;
	}

	public String getPaymentReference() {
		return paymentReference;
	}

	public void setPaymentReference(String paymentReference) {
		this.paymentReference = paymentReference;
	}

	public String getPaymentUrl() {
		return paymentUrl;
	}

	public void setPaymentUrl(String paymentUrl) {
		this.paymentUrl = paymentUrl;
		this.paymentQrCodeUrl = qrCodeUrl(paymentUrl);
	}

	public String getPaymentQrCodeUrl() {
		if (paymentQrCodeUrl != null && !paymentQrCodeUrl.isBlank()) {
			return paymentQrCodeUrl;
		}
		return qrCodeUrl(paymentUrl);
	}

	public void setPaymentQrCodeUrl(String paymentQrCodeUrl) {
		this.paymentQrCodeUrl = paymentQrCodeUrl;
	}

	public Long getTransactionWithdrawalId() {
		return transactionWithdrawalId;
	}

	public TrackerUser getEmployee() {
		return employee;
	}

	public void setEmployee(TrackerUser employee) {
		this.employee = employee;
	}

	public TrackerUser getOwner() {
		return owner;
	}

	public void setOwner(TrackerUser owner) {
		this.owner = owner;
	}

	public Business getBusiness() {
		return business;
	}

	public void setBusiness(Business business) {
		this.business = business;
	}

	public TrackerUser getCustomer() {
		return customer;
	}

	public void setCustomer(TrackerUser customer) {
		this.customer = customer;
	}

	public TuckShopFulfilmentStatus getFulfilmentStatus() {
		return fulfilmentStatus;
	}

	public void setFulfilmentStatus(TuckShopFulfilmentStatus fulfilmentStatus) {
		this.fulfilmentStatus = fulfilmentStatus;
	}

	public String getCollectionToken() {
		return collectionToken;
	}

	public void setCollectionToken(String collectionToken) {
		this.collectionToken = collectionToken;
	}

	public LocalDateTime getCollectionReadyAt() {
		return collectionReadyAt;
	}

	public LocalDateTime getCollectedAt() {
		return collectedAt;
	}

	public Long getPreparedByWorkerId() {
		return preparedByWorkerId;
	}

	public List<TransactionItem> getItems() {
		return items;
	}

	private String qrCodeUrl(String targetUrl) {
		if (targetUrl == null || targetUrl.isBlank()) {
			return null;
		}
		String encodedUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
		return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=%s".formatted(encodedUrl);
	}
}
