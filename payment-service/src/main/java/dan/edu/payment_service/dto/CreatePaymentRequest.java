package dan.edu.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import dan.edu.payment_service.utils.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(
		@NotNull UUID orderId,
		@NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
		@NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code") String currency,
		@NotNull PaymentMethod method,
		@NotBlank @Size(max = 100) String idempotencyKey) {
}
