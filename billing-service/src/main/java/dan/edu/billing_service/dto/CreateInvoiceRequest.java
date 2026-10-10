package dan.edu.billing_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateInvoiceRequest(
		@NotNull UUID orderId,
		@NotNull UUID customerId,
		@NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code") String currency,
		@NotEmpty @Size(max = 100) List<@NotNull @Valid InvoiceLineRequest> lines,
		@DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal taxRate,
		@FutureOrPresent LocalDate dueDate) {
}
