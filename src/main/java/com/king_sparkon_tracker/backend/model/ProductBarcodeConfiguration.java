package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_barcode_configurations")
public class ProductBarcodeConfiguration {

	@Id
private Long productId;

@MapsId
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "product_id")
private Product product;

	@Enumerated(EnumType.STRING)
	@Column(name = "barcode_mode", nullable = false, length = 32)
	private ProductBarcodeMode barcodeMode = ProductBarcodeMode.AUTO_GENERATED;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProductBarcodeConfiguration() {
	}

	public ProductBarcodeConfiguration(Product product, ProductBarcodeMode barcodeMode) {
		this.product = product;
		this.barcodeMode = barcodeMode;
	}

	@PrePersist
void prePersist() {

  
    LocalDateTime now = LocalDateTime.now();

    createdAt = now;
    updatedAt = now;
}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getProductId() {
		return productId;
	}

	public Product getProduct() {
		return product;
	}

	public ProductBarcodeMode getBarcodeMode() {
		return barcodeMode;
	}

	public void setBarcodeMode(ProductBarcodeMode barcodeMode) {
		this.barcodeMode = barcodeMode;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	} public void setProduct(Product product) {
    this.product = product;
}
}
