package dan.edu.order_service.dto;

import java.util.List;
import java.util.UUID;

public record StockReservationPayload(UUID orderId, List<Item> items) {
	public record Item(String sku, int quantity) {
	}
}
