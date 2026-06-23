package com.king_sparkon_tracker.backend.model;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "product_barcodes",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_product_barcodes_barcode",
				columnNames = "barcode"
		)
)
public class ProductBarcode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, unique = true)
	private String barcode;

	@Column(name = "reference_email")
	private String referenceEmail;

	/*
	 * Returnable packaging claim lifecycle.
	 *
	 * NOT_CLAIMED   = returnable barcode can still be claimed.
	 * CLAIMED       = returnable barcode was already claimed.
	 * EXPIRED       = returnable barcode was not claimed before the Friday 17:00 cutoff.
	 * NOT_CLAIMABLE = product is not returnable, so this barcode can never be claimed.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductBarcodeStatus status = ProductBarcodeStatus.NOT_CLAIMED;

	/*
	 * Sale lifecycle.
	 *
	 * AVAILABLE = barcode is in stock and can be sold.
	 * SOLD      = barcode was used in a SELL transaction.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "availability_status", nullable = false)
	private ProductBarcodeAvailabilityStatus availabilityStatus = ProductBarcodeAvailabilityStatus.AVAILABLE;

	protected ProductBarcode() {
	}

	public ProductBarcode(String barcode) {
		this.barcode = barcode;
		this.availabilityStatus = ProductBarcodeAvailabilityStatus.AVAILABLE;
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getReferenceEmail() {
		return referenceEmail;
	}

	public void setReferenceEmail(String referenceEmail) {
		this.referenceEmail = referenceEmail;
	}

	public ProductBarcodeStatus getStatus() {
		return status;
	}

	public void setStatus(ProductBarcodeStatus status) {
		this.status = status;
	}

	public ProductBarcodeAvailabilityStatus getAvailabilityStatus() {
		return availabilityStatus;
	}

	public void setAvailabilityStatus(ProductBarcodeAvailabilityStatus availabilityStatus) {
		this.availabilityStatus = availabilityStatus;
	}
}
