package dan.edu.inventory_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
		UUID id,
		UUID productId,
		String sku,
		BigDecimal price,
		BigDecimal cost,
		int availableQuantity,
		String location,
		Instant createdAt,
		Instant updatedAt) {
}
