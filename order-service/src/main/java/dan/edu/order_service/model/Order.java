package dan.edu.order_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dan.edu.order_service.common.InvalidOrderStateException;
import dan.edu.order_service.dto.ProductSnapshot;
import dan.edu.order_service.dto.OrderItemRequest;
import dan.edu.order_service.dto.OrderRequest;
import dan.edu.order_service.utils.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders",
		indexes = {
				@Index(name = "idx_orders_user_created", columnList = "user_id, created_at"),
				@Index(name = "idx_orders_status", columnList = "status")
		})
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Convert(converter = dan.edu.order_service.model.OrderStatusConverter.class)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	@Column(name = "shipping_address", length = 500)
	private String shippingAddress;

	@Column(length = 1000)
	private String notes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected Order() {
	}

	public Order(UUID userId, OrderRequest request, Map<UUID, ProductSnapshot> products) {
		this.userId = userId;
		this.currency = products.values().iterator().next().currency().toUpperCase(java.util.Locale.ROOT);
		this.shippingAddress = normalizeOptional(request.shippingAddress());
		this.notes = normalizeOptional(request.notes());
		this.status = OrderStatus.PENDING;
		this.totalAmount = BigDecimal.ZERO;
		for (OrderItemRequest itemRequest : request.items()) {
			addItem(itemRequest, products.get(itemRequest.productId()));
		}
	}

	public void addItem(OrderItemRequest request, ProductSnapshot product) {
		if (request == null || product == null) {
			throw new IllegalArgumentException("A valid product and order item are required");
		}
		if (status != OrderStatus.PENDING) {
			throw new InvalidOrderStateException("Only pending orders can be modified");
		}
		OrderItem item = new OrderItem(this, request, product);
		items.add(item);
		totalAmount = totalAmount.add(item.getSubtotal());
	}

	public void updateItems(List<OrderItemRequest> requests, Map<UUID, ProductSnapshot> products) {
		if (status != OrderStatus.PENDING) {
			throw new InvalidOrderStateException("Only pending orders can be modified");
		}
		items.clear();
		totalAmount = BigDecimal.ZERO;
		for (OrderItemRequest request : requests) {
			addItem(request, products.get(request.productId()));
		}
	}

	public void confirm() {
		if (status == OrderStatus.CONFIRMED) {
			return;
		}
		if (status != OrderStatus.PENDING) {
			throw new InvalidOrderStateException("Only pending orders can be confirmed");
		}
		status = OrderStatus.CONFIRMED;
	}

	public void markShipping() {
		if (status == OrderStatus.SHIPPING) {
			return;
		}
		if (status != OrderStatus.CONFIRMED) {
			throw new InvalidOrderStateException("Only confirmed orders can be shipped");
		}
		status = OrderStatus.SHIPPING;
	}

	public void complete() {
		if (status == OrderStatus.COMPLETED) {
			return;
		}
		if (status != OrderStatus.SHIPPING) {
			throw new InvalidOrderStateException("Only shipping orders can be completed");
		}
		status = OrderStatus.COMPLETED;
	}

	public void cancel() {
		if (status == OrderStatus.CANCELLED) {
			return;
		}
		if (status != OrderStatus.PENDING) {
			throw new InvalidOrderStateException("Only pending orders can be cancelled");
		}
		status = OrderStatus.CANCELLED;
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

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public BigDecimal getTotalAmount() { return totalAmount; }
	public String getCurrency() { return currency; }
	public OrderStatus getStatus() { return status; }
	public String getShippingAddress() { return shippingAddress; }
	public String getNotes() { return notes; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public List<OrderItem> getItems() { return items; }
}
