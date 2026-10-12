package dan.edu.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryMovementRequest(
		@NotBlank String sku,
		@NotNull @Min(1) Integer quantity,
		@NotBlank String type,
		String notes) {
}
