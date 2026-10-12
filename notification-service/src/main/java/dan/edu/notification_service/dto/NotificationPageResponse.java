package dan.edu.notification_service.dto;

import java.util.List;

public record NotificationPageResponse(
		List<NotificationResponse> content,
		long totalElements,
		int totalPages,
		int page,
		int size) {
}
