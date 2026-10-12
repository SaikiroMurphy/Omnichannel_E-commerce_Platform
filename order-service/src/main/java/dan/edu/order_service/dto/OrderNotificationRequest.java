package dan.edu.order_service.dto;

import java.util.UUID;

public record OrderNotificationRequest(
		UUID userId,
		String title,
		String message,
		String channel,
		String eventType,
		String referenceId) {
}
