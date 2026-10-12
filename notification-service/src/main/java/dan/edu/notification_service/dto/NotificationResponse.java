package dan.edu.notification_service.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		UUID userId,
		String title,
		String message,
		String channel,
		String eventType,
		String referenceId,
		String status,
		Instant createdAt,
		Instant updatedAt) {
}
