package dan.edu.order_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import dan.edu.order_service.model.Order;

public record OrderCreatedEvent(
		UUID orderId,
		UUID userId,
		BigDecimal totalAmount,
		String currency,
		String status,
		Instant occurredAt) {

	public static OrderCreatedEvent from(Order order) {
		return new OrderCreatedEvent(
				order.getId(),
				order.getUserId(),
				order.getTotalAmount(),
				order.getCurrency(),
				order.getStatus().name(),
				Instant.now());
	}
}
