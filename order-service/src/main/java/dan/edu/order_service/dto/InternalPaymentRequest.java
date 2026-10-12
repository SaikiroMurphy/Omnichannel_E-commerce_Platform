package dan.edu.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalPaymentRequest(
		UUID userId,
		UUID orderId,
		BigDecimal amount,
		String currency,
		String method,
		String idempotencyKey) {
}
