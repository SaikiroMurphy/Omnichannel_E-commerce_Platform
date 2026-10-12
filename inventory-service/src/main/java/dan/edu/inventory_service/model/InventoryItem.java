package dan.edu.inventory_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import dan.edu.inventory_service.common.InvalidInventoryStateException;
import dan.edu.inventory_service.dto.InventoryItemRequest;
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
@Table(name = "inventory_items",
		uniqueConstraints = @UniqueConstraint(name = "uk_inventory_items_sku", columnNames = "sku"),
		indexes = {
				@Index(name = "idx_inventory_items_active_sku", columnList = "active, sku"),
				@Index(name = "idx_inventory_items_location", columnList = "location")
		})
public class InventoryItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 64)
	private String sku;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal cost;

	@Column(nullable = false)
	private int availableQuantity;

	@Column(nullable = false)
	private int reservedQuantity;

	@Column(length = 255)
	private String location;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected InventoryItem() {
	}

	public InventoryItem(InventoryItemRequest request) {
		update(request);
	}

	public void update(InventoryItemRequest request) {
		String requestedSku = normalizeSku(request.sku());
		if (reservedQuantity > 0 && !sku.equals(requestedSku)) {
			throw new InvalidInventoryStateException("SKU cannot be changed while stock is reserved");
		}
		sku = requestedSku;
		price = request.price();
		cost = request.cost();
		availableQuantity = request.availableQuantity();
		if (availableQuantity < 0) {
			throw new IllegalArgumentException("Available quantity cannot be negative");
		}
		location = normalizeOptional(request.location());
	}

	public void addStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Stock quantity must be greater than zero");
		}
		availableQuantity += quantity;
	}

	public void removeStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Stock quantity must be greater than zero");
		}
		if (availableQuantity < quantity) {
			throw new InvalidInventoryStateException("Not enough stock available for the requested decrease");
		}
		availableQuantity -= quantity;
	}

	public void reserveStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Reserved quantity must be greater than zero");
		}
		if (availableQuantity < quantity) {
			throw new InvalidInventoryStateException("Not enough stock to reserve");
		}
		availableQuantity -= quantity;
		reservedQuantity += quantity;
	}

	public void releaseStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Released quantity must be greater than zero");
		}
		if (reservedQuantity < quantity) {
			throw new InvalidInventoryStateException("Not enough stock reserved to release");
		}
		reservedQuantity -= quantity;
		availableQuantity += quantity;
	}

	public void confirmReservedStock(int quantity) {
		if (quantity <= 0 || reservedQuantity < quantity) {
			throw new InvalidInventoryStateException("Not enough stock reserved to confirm");
		}
		reservedQuantity -= quantity;
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

	private static String normalizeSku(String sku) {
		return sku.trim().toUpperCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public int getAvailableQuantity() {
		return availableQuantity;
	}

	public int getReservedQuantity() {
		return reservedQuantity;
	}

	public String getLocation() {
		return location;
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
