package com.king_sparkon_tracker.backend.inventory.reservation;

import java.time.Instant;
import java.util.UUID;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "stock_reservations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_stock_reservation_payment_product",
				columnNames = { "payment_reference", "product_id" }))
public class StockReservation {

	@Id
	@Column(length = 64)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction_id")
	private InventoryTransaction transaction;

	@Column(name = "payment_reference", nullable = false, length = 180)
	private String paymentReference;

	@Column(name = "idempotency_key", length = 128)
	private String idempotencyKey;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_codes", columnDefinition = "text")
	private String unitCodes;

	@Column(name = "reference_email", length = 320)
	private String referenceEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private StockReservationStatus status;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected StockReservation() {
	}

	public StockReservation(
			Business business,
			Product product,
			InventoryTransaction transaction,
			String paymentReference,
			String idempotencyKey,
			int quantity,
			String unitCodes,
			String referenceEmail,
			Instant expiresAt) {
		this.id = "STK-" + UUID.randomUUID();
		this.business = business;
		this.product = product;
		this.transaction = transaction;
		this.paymentReference = paymentReference;
		this.idempotencyKey = idempotencyKey;
		this.quantity = quantity;
		this.unitCodes = unitCodes;
		this.referenceEmail = referenceEmail;
		this.expiresAt = expiresAt;
		this.status = StockReservationStatus.ACTIVE;
	}

	@PrePersist void beforeCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }

	public void bind(InventoryTransaction transaction) { this.transaction = transaction; }
	public void consume() {
		if (status == StockReservationStatus.CONSUMED) return;
		if (status != StockReservationStatus.ACTIVE) throw new IllegalStateException("Only ACTIVE stock reservations can be consumed");
		status = StockReservationStatus.CONSUMED;
	}
	public void release(boolean expired) {
		if (status != StockReservationStatus.ACTIVE) return;
		status = expired ? StockReservationStatus.EXPIRED : StockReservationStatus.RELEASED;
	}

	public String getId() { return id; }
	public Business getBusiness() { return business; }
	public Product getProduct() { return product; }
	public InventoryTransaction getTransaction() { return transaction; }
	public String getPaymentReference() { return paymentReference; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public int getQuantity() { return quantity; }
	public String getUnitCodes() { return unitCodes; }
	public String getReferenceEmail() { return referenceEmail; }
	public StockReservationStatus getStatus() { return status; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
