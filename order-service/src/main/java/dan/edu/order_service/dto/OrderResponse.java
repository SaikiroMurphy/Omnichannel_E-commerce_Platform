package dan.edu.order_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dan.edu.order_service.utils.enums.OrderStatus;

public record OrderResponse(
		UUID id,
		UUID userId,
		BigDecimal totalAmount,
		String currency,
		OrderStatus status,
		String shippingAddress,
		String notes,
		List<OrderItemResponse> items,
		Instant createdAt,
		Instant updatedAt) {
}
