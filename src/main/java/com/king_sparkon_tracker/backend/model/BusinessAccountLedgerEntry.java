package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_account_ledger_entries")
public class BusinessAccountLedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 40)
	private BusinessAccountEntryType entryType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private BusinessAccountEntryStatus status;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
	private BigDecimal balanceAfter;

	@Column(name = "provider", length = 40)
	private String provider;

	@Column(name = "provider_reference", length = 180)
	private String providerReference;

	@Column(name = "checkout_url", length = 2048)
	private String checkoutUrl;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "created_by", nullable = false, length = 255)
	private String createdBy;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	protected BusinessAccountLedgerEntry() {
	}

	public BusinessAccountLedgerEntry(
			Business business,
			BusinessAccountEntryType entryType,
			BusinessAccountEntryStatus status,
			BigDecimal amount,
			BigDecimal balanceAfter,
			String provider,
			String providerReference,
			String checkoutUrl,
			String description,
			String createdBy) {
		this.business = business;
		this.entryType = entryType;
		this.status = status;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.provider = provider;
		this.providerReference = providerReference;
		this.checkoutUrl = checkoutUrl;
		this.description = description;
		this.createdBy = createdBy;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		if (modifiedDate == null) {
			modifiedDate = now;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		modifiedDate = OffsetDateTime.now();
	}

	public void markPosted(BigDecimal balanceAfter) {
		this.status = BusinessAccountEntryStatus.POSTED;
		this.balanceAfter = balanceAfter;
	}

	public Long getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public BusinessAccountEntryType getEntryType() {
		return entryType;
	}

	public BusinessAccountEntryStatus getStatus() {
		return status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderReference() {
		return providerReference;
	}

	public String getCheckoutUrl() {
		return checkoutUrl;
	}

	public String getDescription() {
		return description;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}
}
