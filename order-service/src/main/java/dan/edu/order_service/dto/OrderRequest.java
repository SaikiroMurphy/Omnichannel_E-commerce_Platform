package dan.edu.order_service.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderRequest(
		@NotNull @Size(min = 1, max = 50) List<OrderItemRequest> items,
		@Size(max = 500) String shippingAddress,
		@Size(max = 1000) String notes,
		@Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code") String currency) {
}
