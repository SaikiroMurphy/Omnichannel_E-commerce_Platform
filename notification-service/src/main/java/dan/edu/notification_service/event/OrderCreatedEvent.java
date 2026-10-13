package dan.edu.notification_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
		UUID orderId,
		UUID userId,
		BigDecimal totalAmount,
		String currency,
		String status,
		Instant occurredAt) {
}
