package dan.edu.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPaymentEvent(
		UUID userId,
		UUID orderId,
		BigDecimal amount,
		String currency,
		String status) {
}
