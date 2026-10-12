package dan.edu.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderPaymentRequest(
		@NotBlank String method,
		@NotBlank @Size(max = 100) String idempotencyKey) {
}
