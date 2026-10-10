package dan.edu.product_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import dan.edu.product_service.dto.ProductRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "products",
		uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
		indexes = {
				@Index(name = "idx_products_active_category", columnList = "active, category"),
				@Index(name = "idx_products_name", columnList = "name")
		})
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 64)
	private String sku;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(nullable = false, length = 80)
	private String category;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Product() {
	}

	public Product(ProductRequest request) {
		update(request);
	}

	public void update(ProductRequest request) {
		sku = request.sku().trim().toUpperCase(Locale.ROOT);
		name = request.name().trim();
		category = request.category().trim();
		description = normalizeOptional(request.description());
		price = request.price();
		currency = request.currency().trim().toUpperCase(Locale.ROOT);
		imageUrl = normalizeOptional(request.imageUrl());
	}

	public void deactivate() {
		active = false;
	}

	public void activate() {
		active = true;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	private static String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public UUID getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getCurrency() {
		return currency;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
