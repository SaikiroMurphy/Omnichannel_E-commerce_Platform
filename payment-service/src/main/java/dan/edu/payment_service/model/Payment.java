package dan.edu.payment_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import dan.edu.payment_service.common.ConflictException;
import dan.edu.payment_service.common.InvalidPaymentStateException;
import dan.edu.payment_service.utils.enums.PaymentMethod;
import dan.edu.payment_service.utils.enums.PaymentOutcome;
import dan.edu.payment_service.utils.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "payments",
		uniqueConstraints = @UniqueConstraint(name = "uk_payments_user_idempotency",
				columnNames = {"user_id", "idempotency_key"}),
		indexes = {
				@Index(name = "idx_payments_user_created", columnList = "user_id, created_at"),
				@Index(name = "idx_payments_order", columnList = "order_id")
		})
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "order_id", nullable = false, updatable = false)
	private UUID orderId;

	@Column(nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false, length = 3, updatable = false)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 30, updatable = false)
	private PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "idempotency_key", nullable = false, length = 100, updatable = false)
	private String idempotencyKey;

	@Column(name = "failure_reason", length = 200)
	private String failureReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Payment() {
	}

	public Payment(
			UUID userId,
			UUID orderId,
			BigDecimal amount,
			String currency,
			PaymentMethod method,
			String idempotencyKey) {
		this.userId = userId;
		this.orderId = orderId;
		this.amount = amount;
		this.currency = currency;
		this.method = method;
		this.idempotencyKey = idempotencyKey;
		this.status = PaymentStatus.PENDING;
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

	public void simulate(PaymentOutcome outcome) {
		if (status == PaymentStatus.SUCCEEDED && outcome == PaymentOutcome.APPROVED
				|| status == PaymentStatus.FAILED && outcome == PaymentOutcome.DECLINED) {
			return;
		}
		if (status != PaymentStatus.PENDING) {
			throw new InvalidPaymentStateException("Only pending payments can be simulated");
		}
		status = outcome == PaymentOutcome.APPROVED ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
		failureReason = outcome == PaymentOutcome.DECLINED ? "Mock payment was declined" : null;
	}

	public void cancel() {
		if (status == PaymentStatus.CANCELLED) {
			return;
		}
		if (status != PaymentStatus.PENDING) {
			throw new InvalidPaymentStateException("Only pending payments can be cancelled");
		}
		status = PaymentStatus.CANCELLED;
	}

	public void refund() {
		if (status == PaymentStatus.REFUNDED) {
			return;
		}
		if (status != PaymentStatus.SUCCEEDED) {
			throw new InvalidPaymentStateException("Only successful payments can be refunded");
		}
		status = PaymentStatus.REFUNDED;
	}

	public boolean matches(
			UUID userId,
			UUID orderId,
			BigDecimal amount,
			String currency,
			PaymentMethod method) {
		return this.userId.equals(userId)
				&& this.orderId.equals(orderId)
				&& this.amount.compareTo(amount) == 0
				&& this.currency.equals(currency)
				&& this.method == method;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public PaymentMethod getMethod() {
		return method;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
