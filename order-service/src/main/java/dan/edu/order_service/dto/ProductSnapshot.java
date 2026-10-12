package dan.edu.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSnapshot(
		UUID id,
		String sku,
		String name,
		BigDecimal price,
		String currency,
		boolean active) {
}
