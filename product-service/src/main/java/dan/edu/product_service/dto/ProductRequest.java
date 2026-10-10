package dan.edu.product_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String sku,
		@NotBlank @Size(max = 160) String name,
		@NotBlank @Size(max = 80) String category,
		@Size(max = 2000) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
		@NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code") String currency,
		@Size(max = 2048) @Pattern(regexp = "^$|^https?://[^\\s]+$", message = "must be an absolute HTTP(S) URL")
		String imageUrl) {
}
