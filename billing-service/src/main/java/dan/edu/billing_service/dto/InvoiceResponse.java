package dan.edu.billing_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dan.edu.billing_service.utils.enums.InvoiceStatus;

public record InvoiceResponse(
		UUID id,
		String invoiceNumber,
		UUID orderId,
		UUID customerId,
		String currency,
		List<InvoiceLineResponse> lines,
		BigDecimal subtotal,
		BigDecimal taxRate,
		BigDecimal taxAmount,
		BigDecimal total,
		LocalDate dueDate,
		InvoiceStatus status,
		Instant issuedAt,
		Instant voidedAt,
		Instant createdAt,
		Instant updatedAt) {
}
