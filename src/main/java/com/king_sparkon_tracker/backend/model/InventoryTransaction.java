package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

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

	@Column(name = "payment_reference")
	private String paymentReference;

	@Column(name = "payment_url", length = 2048)
	private String paymentUrl;

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
	}

	public void addItem(TransactionItem item) {
		item.setTransaction(this);
		items.add(item);
	}

	public void markOfflinePayment(TransactionPaymentType paymentType, String paymentEmail) {
		this.paymentType = paymentType;
		this.paymentStatus = TransactionPaymentStatus.NOT_REQUIRED;
		this.paymentEmail = paymentEmail;
		this.paymentReference = null;
		this.paymentUrl = null;
	}

	public void prepareWebsitePayment(String paymentEmail) {
		this.paymentType = TransactionPaymentType.WEBSITE_PAYMENT;
		this.paymentStatus = TransactionPaymentStatus.PENDING;
		this.paymentEmail = paymentEmail;
	}

	public void markWebsitePaymentPending(String paymentEmail, String paymentReference, String paymentUrl) {
		this.paymentType = TransactionPaymentType.WEBSITE_PAYMENT;
		this.paymentStatus = TransactionPaymentStatus.PENDING;
		this.paymentEmail = paymentEmail;
		this.paymentReference = paymentReference;
		this.paymentUrl = paymentUrl;
	}

	public void markWebsitePaymentPaid(String paymentReference) {
		this.paymentType = TransactionPaymentType.WEBSITE_PAYMENT;
		this.paymentStatus = TransactionPaymentStatus.PAID;
		if ((this.paymentReference == null || this.paymentReference.isBlank()) && paymentReference != null && !paymentReference.isBlank()) {
			this.paymentReference = paymentReference;
		}
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

	public List<TransactionItem> getItems() {
		return items;
	}
}
