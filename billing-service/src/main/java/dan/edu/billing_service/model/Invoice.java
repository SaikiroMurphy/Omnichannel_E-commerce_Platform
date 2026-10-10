package dan.edu.billing_service.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dan.edu.billing_service.common.InvalidInvoiceStateException;
import dan.edu.billing_service.dto.CreateInvoiceRequest;
import dan.edu.billing_service.utils.enums.InvoiceStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "invoices",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_invoices_number", columnNames = "invoice_number"),
				@UniqueConstraint(name = "uk_invoices_order", columnNames = "order_id")
		},
		indexes = @Index(name = "idx_invoices_customer_created", columnList = "customer_id, created_at"))
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "invoice_number", nullable = false, unique = true, length = 40)
	private String invoiceNumber;

	@Column(name = "order_id", nullable = false, updatable = false)
	private UUID orderId;

	@Column(name = "customer_id", nullable = false, updatable = false)
	private UUID customerId;

	@Column(nullable = false, length = 3, updatable = false)
	private String currency;

	@ElementCollection
	@CollectionTable(name = "invoice_lines", joinColumns = @JoinColumn(name = "invoice_id"))
	@OrderColumn(name = "line_number")
	private List<InvoiceLine> lines = new ArrayList<>();

	@Column(nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal subtotal;

	@Column(name = "tax_rate", nullable = false, precision = 5, scale = 2, updatable = false)
	private BigDecimal taxRate;

	@Column(name = "tax_amount", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal taxAmount;

	@Column(nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal total;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvoiceStatus status;

	@Column(name = "issued_at")
	private Instant issuedAt;

	@Column(name = "voided_at")
	private Instant voidedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Invoice() {
	}

	public Invoice(CreateInvoiceRequest request) {
		orderId = request.orderId();
		customerId = request.customerId();
		currency = request.currency().trim().toUpperCase(Locale.ROOT);
		dueDate = request.dueDate();
		taxRate = request.taxRate() == null ? BigDecimal.ZERO : request.taxRate();
		lines = new ArrayList<>(request.lines().stream().map(InvoiceLine::new).toList());
		subtotal = lines.stream()
				.map(InvoiceLine::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		taxAmount = subtotal.multiply(taxRate)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
		status = InvoiceStatus.DRAFT;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		invoiceNumber = "INV-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void issue() {
		if (status == InvoiceStatus.ISSUED) {
			return;
		}
		if (status != InvoiceStatus.DRAFT) {
			throw new InvalidInvoiceStateException("Only draft invoices can be issued");
		}
		status = InvoiceStatus.ISSUED;
		issuedAt = Instant.now();
	}

	public void voidInvoice() {
		if (status == InvoiceStatus.VOID) {
			return;
		}
		if (status != InvoiceStatus.DRAFT && status != InvoiceStatus.ISSUED) {
			throw new InvalidInvoiceStateException("This invoice cannot be voided");
		}
		status = InvoiceStatus.VOID;
		voidedAt = Instant.now();
	}

	public boolean belongsTo(UUID userId) {
		return customerId.equals(userId);
	}

	public UUID getId() {
		return id;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public String getCurrency() {
		return currency;
	}

	public List<InvoiceLine> getLines() {
		return List.copyOf(lines);
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getTaxRate() {
		return taxRate;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public InvoiceStatus getStatus() {
		return status;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getVoidedAt() {
		return voidedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
