package dan.edu.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentOrderEvent(
		UUID userId,
		UUID orderId,
		BigDecimal amount,
		String currency,
		String status) {
}
