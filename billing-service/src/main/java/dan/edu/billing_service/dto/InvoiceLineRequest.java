package dan.edu.billing_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InvoiceLineRequest(
		@NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String sku,
		@NotBlank @Size(max = 200) String description,
		@Min(1) @Max(10000) int quantity,
		@NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice) {
}
