package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "products",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_products_business_product_barcode",
				columnNames = { "business_id", "product_barcode" }
		)
)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "product_barcode", length = 128)
	private String productBarcode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "barcode_catalog_id")
	private BarcodeCatalog barcodeCatalog;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductStatus status = ProductStatus.CREATED;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "product_image_url", length = 2048)
	private String productImageUrl;

	@Column(name = "returnable_enabled", nullable = false)
	private boolean returnableEnabled;

	@Column(name = "returnable_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal returnablePrice = BigDecimal.ZERO;

	@Column(name = "night_shift_enabled", nullable = false)
	private boolean nightShiftEnabled;

	@Column(name = "night_shift_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal nightShiftPrice = BigDecimal.ZERO;

	@Column(name = "night_shift_start_time")
	private LocalTime nightShiftStartTime;

	@Column(name = "night_shift_end_time")
	private LocalTime nightShiftEndTime;

	@Column(nullable = false)
	private int stockQuantity;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "business_id")
	private Business business;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("unitCode asc")
	private List<ProductBarcode> barcodes = new ArrayList<>();

	@OneToMany(mappedBy = "product")
	private List<TransactionItem> transactionItems = new ArrayList<>();

	protected Product() {
	}

	public Product(String name, ProductCategory category, BigDecimal price, int stockQuantity) {
		this(
				name,
				category,
				price,
				stockQuantity,
				false,
				BigDecimal.ZERO,
				false,
				BigDecimal.ZERO,
				null,
				null,
				null
		);
	}

	public Product(
			String name,
			ProductCategory category,
			BigDecimal price,
			int stockQuantity,
			boolean returnableEnabled) {
		this(
				name,
				category,
				price,
				stockQuantity,
				returnableEnabled,
				BigDecimal.ZERO,
				false,
				BigDecimal.ZERO,
				null,
				null,
				null
		);
	}

	public Product(
			String name,
			ProductCategory category,
			BigDecimal price,
			int stockQuantity,
			boolean returnableEnabled,
			Business business) {
		this(
				name,
				category,
				price,
				stockQuantity,
				returnableEnabled,
				BigDecimal.ZERO,
				false,
				BigDecimal.ZERO,
				null,
				null,
				business
		);
	}

	public Product(
			String name,
			ProductCategory category,
			BigDecimal price,
			int stockQuantity,
			boolean returnableEnabled,
			BigDecimal returnablePrice,
			boolean nightShiftEnabled,
			BigDecimal nightShiftPrice,
			LocalTime nightShiftStartTime,
			LocalTime nightShiftEndTime,
			Business business) {
		this.name = name;
		this.category = category;
		this.price = price;
		this.stockQuantity = stockQuantity;
		this.returnableEnabled = returnableEnabled;
		this.returnablePrice = returnablePrice == null ? BigDecimal.ZERO : returnablePrice;
		this.nightShiftEnabled = nightShiftEnabled;
		this.nightShiftPrice = nightShiftPrice == null ? BigDecimal.ZERO : nightShiftPrice;
		this.nightShiftStartTime = nightShiftStartTime;
		this.nightShiftEndTime = nightShiftEndTime;
		this.business = business;
	}

	public Product(String name, String barcode, ProductCategory category, BigDecimal price, int stockQuantity) {
		this(name, barcode, category, price, stockQuantity, false);
	}

	public Product(
			String name,
			String barcode,
			ProductCategory category,
			BigDecimal price,
			int stockQuantity,
			boolean returnableEnabled) {
		this(
				name,
				barcode,
				category,
				price,
				stockQuantity,
				returnableEnabled,
				BigDecimal.ZERO,
				false,
				BigDecimal.ZERO,
				null,
				null,
				null
		);
	}

	public Product(
			String name,
			String barcode,
			ProductCategory category,
			BigDecimal price,
			int stockQuantity,
			boolean returnableEnabled,
			Business business) {
		this(
				name,
				barcode,
				category,
				price,
				stockQuantity,
				returnableEnabled,
				BigDecimal.ZERO,
				false,
				BigDecimal.ZERO,
				null,
				null,
				business
		);
	}

	public Product(
			String name,
			String barcode,
			ProductCategory category,
			BigDecimal price,
			int stockQuantity,
			boolean returnableEnabled,
			BigDecimal returnablePrice,
			boolean nightShiftEnabled,
			BigDecimal nightShiftPrice,
			LocalTime nightShiftStartTime,
			LocalTime nightShiftEndTime,
			Business business) {
		this(
				name,
				category,
				price,
				stockQuantity,
				returnableEnabled,
				returnablePrice,
				nightShiftEnabled,
				nightShiftPrice,
				nightShiftStartTime,
				nightShiftEndTime,
				business
		);
		this.productBarcode = barcode;
		ProductBarcode productBarcode = new ProductBarcode(barcode);
		productBarcode.setStatus(initialClaimStatus());
		addBarcode(productBarcode);
	}

	private ProductBarcodeStatus initialClaimStatus() {
		return returnableEnabled ? ProductBarcodeStatus.NOT_CLAIMED : ProductBarcodeStatus.NOT_CLAIMABLE;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBarcode() {
		return productBarcode;
	}

	public String getProductBarcode() {
		return productBarcode;
	}

	public void setProductBarcode(String productBarcode) {
		this.productBarcode = productBarcode;
	}

	public BarcodeCatalog getBarcodeCatalog() {
		return barcodeCatalog;
	}

	public void setBarcodeCatalog(BarcodeCatalog barcodeCatalog) {
		this.barcodeCatalog = barcodeCatalog;
	}

	public ProductCategory getCategory() {
		return category;
	}

	public void setCategory(ProductCategory category) {
		this.category = category;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public void setStatus(ProductStatus status) {
		this.status = status;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getProductImageUrl() {
		return productImageUrl;
	}

	public void setProductImageUrl(String productImageUrl) {
		this.productImageUrl = productImageUrl == null || productImageUrl.isBlank()
				? null
				: productImageUrl.trim();
	}

	public boolean isReturnableEnabled() {
		return returnableEnabled;
	}

	public void setReturnableEnabled(boolean returnableEnabled) {
		this.returnableEnabled = returnableEnabled;
	}

	public BigDecimal getReturnablePrice() {
		return returnablePrice;
	}

	public void setReturnablePrice(BigDecimal returnablePrice) {
		this.returnablePrice = returnablePrice == null ? BigDecimal.ZERO : returnablePrice;
	}

	public boolean isNightShiftEnabled() {
		return nightShiftEnabled;
	}

	public void setNightShiftEnabled(boolean nightShiftEnabled) {
		this.nightShiftEnabled = nightShiftEnabled;
	}

	public BigDecimal getNightShiftPrice() {
		return nightShiftPrice;
	}

	public void setNightShiftPrice(BigDecimal nightShiftPrice) {
		this.nightShiftPrice = nightShiftPrice == null ? BigDecimal.ZERO : nightShiftPrice;
	}

	public LocalTime getNightShiftStartTime() {
		return nightShiftStartTime;
	}

	public void setNightShiftStartTime(LocalTime nightShiftStartTime) {
		this.nightShiftStartTime = nightShiftStartTime;
	}

	public LocalTime getNightShiftEndTime() {
		return nightShiftEndTime;
	}

	public void setNightShiftEndTime(LocalTime nightShiftEndTime) {
		this.nightShiftEndTime = nightShiftEndTime;
	}

	public boolean isBottleReturnable() {
		return returnableEnabled;
	}

	public void setBottleReturnable(boolean bottleReturnable) {
		this.returnableEnabled = bottleReturnable;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(int stockQuantity) {
		this.stockQuantity = stockQuantity;
	}

	public Business getBusiness() {
		return business;
	}

	public void setBusiness(Business business) {
		this.business = business;
	}

	public List<ProductBarcode> getBarcodes() {
		return barcodes;
	}

	public void addBarcode(ProductBarcode barcode) {
		barcode.setProduct(this);
		if (barcode.getBarcode() == null) {
			barcode.setBarcode(productBarcode);
		}
		barcodes.add(barcode);
	}

	public List<TransactionItem> getTransactionItems() {
		return transactionItems;
	}
}
