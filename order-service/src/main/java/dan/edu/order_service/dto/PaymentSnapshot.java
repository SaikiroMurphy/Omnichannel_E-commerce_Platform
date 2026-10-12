package dan.edu.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSnapshot(UUID id, UUID orderId, BigDecimal amount, String currency, String status) {
}
