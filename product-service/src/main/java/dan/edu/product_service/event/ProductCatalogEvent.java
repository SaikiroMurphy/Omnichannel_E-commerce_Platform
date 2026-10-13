package dan.edu.product_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import dan.edu.product_service.model.Product;

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

	public static ProductCatalogEvent created(Product product) {
		return of(CREATED, product, product.getSku());
	}

	public static ProductCatalogEvent updated(Product product, String previousSku) {
		return of(UPDATED, product, previousSku);
	}

	public static ProductCatalogEvent deactivated(Product product) {
		return of(DEACTIVATED, product, product.getSku());
	}

	public static ProductCatalogEvent activated(Product product) {
		return of(ACTIVATED, product, product.getSku());
	}

	private static ProductCatalogEvent of(String type, Product product, String previousSku) {
		return new ProductCatalogEvent(
				type,
				product.getId(),
				product.getSku(),
				previousSku,
				product.getPrice(),
				product.isActive(),
				Instant.now());
	}
}
