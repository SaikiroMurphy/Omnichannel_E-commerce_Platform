package dan.edu.payment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import dan.edu.payment_service.utils.enums.PaymentMethod;
import dan.edu.payment_service.utils.enums.PaymentStatus;

public record PaymentResponse(
		UUID id,
		UUID orderId,
		BigDecimal amount,
		String currency,
		PaymentMethod method,
		PaymentStatus status,
		String failureReason,
		Instant createdAt,
		Instant updatedAt,
		boolean sandbox) {
}
