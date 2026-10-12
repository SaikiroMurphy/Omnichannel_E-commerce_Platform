package dan.edu.notification_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
		@NotNull UUID userId,
		@NotBlank @Size(max = 255) String title,
		@NotBlank @Size(max = 2000) String message,
		@Size(max = 255) String channel,
		@Size(max = 255) String eventType,
		@Size(max = 255) String referenceId) {
}
