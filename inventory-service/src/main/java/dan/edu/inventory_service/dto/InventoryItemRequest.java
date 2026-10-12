package dan.edu.inventory_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryItemRequest(
		@NotBlank @Size(max = 64) String sku,
		@NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
		@NotNull @DecimalMin("0.00") @Digits(integer = 18, fraction = 2) BigDecimal cost,
		@NotNull @Min(0) Integer availableQuantity,
		@Size(max = 500) String location) {
}
