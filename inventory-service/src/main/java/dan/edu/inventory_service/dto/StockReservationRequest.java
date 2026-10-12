package dan.edu.inventory_service.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockReservationRequest(
		@NotNull UUID orderId,
		@NotNull @Size(min = 1, max = 50) List<@Valid Item> items) {

	public record Item(@NotBlank @Size(max = 64) String sku, @NotNull @Min(1) Integer quantity) {
	}
}
