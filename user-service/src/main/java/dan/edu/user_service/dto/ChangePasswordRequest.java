package dan.edu.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank @Size(max = 256) String currentPassword,
		@NotBlank @Size(min = 12, max = 72) String newPassword) {
}
