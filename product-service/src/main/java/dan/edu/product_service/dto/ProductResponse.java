package dan.edu.product_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String sku,
		String name,
		String category,
		String description,
		BigDecimal price,
		String currency,
		String imageUrl,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
