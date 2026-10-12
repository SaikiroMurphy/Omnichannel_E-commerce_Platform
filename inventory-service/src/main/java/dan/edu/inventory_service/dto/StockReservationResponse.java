package dan.edu.inventory_service.dto;

import java.util.UUID;

public record StockReservationResponse(UUID orderId, String status) {
}
