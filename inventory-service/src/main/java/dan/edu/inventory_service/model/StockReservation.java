package dan.edu.inventory_service.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_reservations",
		uniqueConstraints = @UniqueConstraint(name = "uk_stock_reservation_order_sku", columnNames = {"order_id", "sku"}),
		indexes = @Index(name = "idx_stock_reservations_order", columnList = "order_id"))
public class StockReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "order_id", nullable = false, updatable = false)
	private UUID orderId;

	@Column(nullable = false, length = 64, updatable = false)
	private String sku;

	@Column(nullable = false, updatable = false)
	private int quantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Version
	private long version;

	protected StockReservation() {
	}

	public StockReservation(UUID orderId, String sku, int quantity) {
		this.orderId = orderId;
		this.sku = sku;
		this.quantity = quantity;
		this.status = ReservationStatus.RESERVED;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void confirm() {
		status = ReservationStatus.CONFIRMED;
	}

	public void release() {
		status = ReservationStatus.RELEASED;
	}

	public UUID getOrderId() { return orderId; }
	public String getSku() { return sku; }
	public int getQuantity() { return quantity; }
	public ReservationStatus getStatus() { return status; }
}
