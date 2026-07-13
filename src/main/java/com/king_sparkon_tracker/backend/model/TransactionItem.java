package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_items")
public class TransactionItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false)
	private InventoryTransaction transaction;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "transaction_item_barcodes", joinColumns = @JoinColumn(name = "transaction_item_id"))
	@OrderColumn(name = "barcode_order")
	@Column(name = "barcode", nullable = false)
	private List<String> barcodes = new ArrayList<>();

	protected TransactionItem() {
	}

	public TransactionItem(Product product, int quantity, BigDecimal unitPrice) {
		this(product, quantity, unitPrice, List.of());
	}

	public TransactionItem(Product product, int quantity, BigDecimal unitPrice, List<String> barcodes) {
		this.product = product;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		setBarcodes(barcodes);
	}

	public Long getId() {
		return id;
	}

	public InventoryTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(InventoryTransaction transaction) {
		this.transaction = transaction;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public List<String> getBarcodes() {
		return barcodes;
	}

	public void setBarcodes(List<String> barcodes) {
		this.barcodes.clear();
		if (barcodes != null) {
			this.barcodes.addAll(barcodes);
		}
	}

	public void addBarcode(String barcode) {
		if (barcode == null || barcode.isBlank()) {
			throw new IllegalArgumentException("Barcode is required");
		}
		if (barcodes.size() >= quantity) {
			throw new IllegalStateException("All purchased units already have assigned barcodes");
		}
		String normalized = barcode.trim();
		if (barcodes.contains(normalized)) {
			throw new IllegalArgumentException("Barcode is already assigned to this purchase item");
		}
		barcodes.add(normalized);
	}
}
