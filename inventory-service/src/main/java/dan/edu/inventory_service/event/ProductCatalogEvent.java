package dan.edu.inventory_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductCatalogEvent(
		String type,
		UUID productId,
		String sku,
		String previousSku,
		BigDecimal price,
		boolean active,
		Instant occurredAt) {

	public static final String CREATED = "CREATED";
	public static final String UPDATED = "UPDATED";
	public static final String DEACTIVATED = "DEACTIVATED";
	public static final String ACTIVATED = "ACTIVATED";
}
